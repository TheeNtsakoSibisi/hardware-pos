package za.co.ntsumo.pos.sales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ntsumo.pos.accounts.*;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.catalogue.ProductRepository;
import za.co.ntsumo.pos.catalogue.StockMovement;
import za.co.ntsumo.pos.catalogue.StockMovementRepository;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.web.SaleRequests;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Captures sales.
 *
 * <p>Both paths end in the same {@link Sale} record. What differs is settlement:
 * a walk-in sale must be covered by its tenders, an account sale must pass the
 * credit rules or carry a manager's override.
 */
@Service
public class SaleService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Africa/Johannesburg");

    private final SaleRepository sales;
    private final ProductRepository products;
    private final StockMovementRepository stock;
    private final ContractorAccountRepository accounts;
    private final AuthorisedBuyerRepository buyers;
    private final JobSiteRepository jobs;
    private final PricingService pricing;
    private final CreditCheckService creditCheck;
    private final DocumentNumberService documentNumbers;
    private final Clock clock;

    public SaleService(SaleRepository sales, ProductRepository products, StockMovementRepository stock,
                       ContractorAccountRepository accounts, AuthorisedBuyerRepository buyers,
                       JobSiteRepository jobs, PricingService pricing, CreditCheckService creditCheck,
                       DocumentNumberService documentNumbers, Clock clock) {
        this.sales = sales;
        this.products = products;
        this.stock = stock;
        this.accounts = accounts;
        this.buyers = buyers;
        this.jobs = jobs;
        this.pricing = pricing;
        this.creditCheck = creditCheck;
        this.documentNumbers = documentNumbers;
        this.clock = clock;
    }

    /** Thrown when a sale cannot be captured as asked. Maps to HTTP 422. */
    public static class SaleRejected extends RuntimeException {
        private final List<String> reasons;

        public SaleRejected(String message, List<String> reasons) {
            super(message);
            this.reasons = List.copyOf(reasons);
        }

        public List<String> getReasons() {
            return reasons;
        }
    }

    /**
     * Captures a walk-in sale.
     *
     * <p>Idempotent on the till's own UUID: a sale queued offline and sent twice
     * after the network returns is stored once.
     */
    @Transactional
    public Sale captureWalkIn(SaleRequests.WalkInSale request, String capturedBy) {
        Optional<Sale> existing = sales.findByClientUuid(request.clientUuid());
        if (existing.isPresent()) {
            return alreadyCaptured(existing.get());
        }

        List<SaleLine> lines = buildLines(request.lines(), null);
        BigDecimal total = totalOf(lines);

        BigDecimal tendered = Money.scaled(request.tenders().stream()
                .map(SaleRequests.Tender::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (tendered.compareTo(total) < 0) {
            throw new SaleRejected("The payment does not cover the sale",
                    List.of("Tendered R%s against a total of R%s."
                            .formatted(tendered.toPlainString(), total.toPlainString())));
        }

        List<SaleTender> tenders = new ArrayList<>();
        for (SaleRequests.Tender tender : request.tenders()) {
            BigDecimal change = null;
            BigDecimal received = null;
            if (tender.type() == TenderType.CASH && tender.cashReceived() != null) {
                received = Money.scaled(tender.cashReceived());
                change = Money.scaled(received.subtract(tender.amount()).max(BigDecimal.ZERO));
            }
            tenders.add(new SaleTender(tender.type(), Money.scaled(tender.amount()),
                    tender.reference(), received, change));
        }

        Sale sale = Sale.walkIn(request.clientUuid(), documentNumbers.nextSaleNumber(request.tillId()),
                request.tillId(), capturedBy, occurredAt(request.occurredAt()), lines, tenders,
                request.customerName(), request.customerVatNo());

        Sale saved = sales.save(sale);
        recordStock(saved);
        return saved;
    }

    /**
     * Charges a sale to a contractor account.
     *
     * <p>The buyer's PIN is checked first, then the credit rules. A blocked sale
     * is only captured when a manager override accompanies it, and the reason is
     * stored on the invoice.
     */
    @Transactional
    public Sale chargeToAccount(SaleRequests.AccountSale request, String capturedBy) {
        Optional<Sale> existing = sales.findByClientUuid(request.clientUuid());
        if (existing.isPresent()) {
            return alreadyCaptured(existing.get());
        }

        ContractorAccount account = accounts.findByCode(request.accountCode())
                .orElseThrow(() -> new SaleRejected("Unknown account",
                        List.of("No account with code " + request.accountCode() + ".")));
        AuthorisedBuyer buyer = buyers.findById(request.buyerId())
                .filter(b -> b.getAccountId().equals(account.getId()))
                .orElseThrow(() -> new SaleRejected("Unknown buyer",
                        List.of("That buyer is not on this account.")));

        if (!creditCheck.pinMatches(buyer, request.buyerPin())) {
            throw new SaleRejected("Buyer PIN does not match",
                    List.of("The PIN entered does not match %s.".formatted(buyer.getName())));
        }

        JobSite job = jobs.findByCode(request.jobCode())
                .filter(j -> j.getAccountId().equals(account.getId()))
                .orElseThrow(() -> new SaleRejected("Unknown job",
                        List.of("That job is not on this account.")));

        List<SaleLine> lines = buildLines(request.lines(), account);
        BigDecimal total = totalOf(lines);

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, total);
        boolean overridden = request.overrideReason() != null && !request.overrideReason().isBlank();
        if (decision.requiresOverride() && !overridden) {
            throw new SaleRejected("Credit check failed",
                    decision.refusals().stream().map(CreditCheckService.Refusal::message).toList());
        }

        LocalDate dueDate = LocalDate.now(clock.withZone(STORE_ZONE)).plusDays(account.getTermsDays());
        Sale sale = Sale.onAccount(request.clientUuid(), documentNumbers.nextInvoiceNumber(request.tillId()),
                request.tillId(), capturedBy, occurredAt(request.occurredAt()), lines,
                account.getId(), buyer.getId(), job.getId(), request.orderRef(), dueDate);

        if (decision.requiresOverride()) {
            sale.recordOverride(request.overrideBy() == null ? capturedBy : request.overrideBy(),
                    request.overrideReason());
        }

        Sale saved = sales.save(sale);
        recordStock(saved);
        return saved;
    }

    /**
     * A sale the till is replaying. Its lines and tenders are loaded here, inside
     * the transaction, because the caller renders the document after the
     * transaction has closed and open-in-view is off.
     */
    private Sale alreadyCaptured(Sale sale) {
        sale.getLines().size();
        sale.getTenders().size();
        return sale;
    }

    private List<SaleLine> buildLines(List<SaleRequests.Line> requested, ContractorAccount account) {
        if (requested == null || requested.isEmpty()) {
            throw new SaleRejected("A sale needs at least one line", List.of("No items on the sale."));
        }
        List<SaleLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (SaleRequests.Line line : requested) {
            Product product = products.findBySku(line.sku())
                    .orElseThrow(() -> new SaleRejected("Unknown product",
                            List.of("No product with SKU " + line.sku() + ".")));
            if (!Money.isPositive(line.qty())) {
                throw new SaleRejected("Quantity must be greater than zero",
                        List.of("%s has a quantity of %s.".formatted(product.getName(), line.qty())));
            }
            BigDecimal unitPrice = pricing.unitPriceFor(product, account);
            lines.add(new SaleLine(lineNo++, product.getId(), product.getName(), line.qty(),
                    product.getUnit(), unitPrice, Money.lineTotal(unitPrice, line.qty())));
        }
        return lines;
    }

    private BigDecimal totalOf(List<SaleLine> lines) {
        return Money.scaled(lines.stream()
                .map(SaleLine::getLineTotalInclVat)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Stock leaves the shelf as a movement, never as an update to a count. */
    private void recordStock(Sale sale) {
        for (SaleLine line : sale.getLines()) {
            stock.save(new StockMovement(line.getProductId(), line.getQty().negate(),
                    StockMovement.Reason.SALE, sale.getDocumentNo()));
        }
    }

    /** Honours the till's own timestamp for offline sales, within reason. */
    private Instant occurredAt(Instant fromTill) {
        Instant now = clock.instant();
        if (fromTill == null || fromTill.isAfter(now)) {
            return now;
        }
        return fromTill;
    }
}
