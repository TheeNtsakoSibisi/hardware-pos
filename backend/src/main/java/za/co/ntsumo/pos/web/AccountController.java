package za.co.ntsumo.pos.web;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import za.co.ntsumo.pos.accounts.*;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.payments.AccountPayment;
import za.co.ntsumo.pos.payments.AccountPaymentRepository;
import za.co.ntsumo.pos.payments.PaymentAllocationService;
import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** Contractor accounts: standing, statements and payments received. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final ZoneId STORE_ZONE = ZoneId.of("Africa/Johannesburg");

    private final ContractorAccountRepository accounts;
    private final AuthorisedBuyerRepository buyers;
    private final JobSiteRepository jobs;
    private final SaleRepository sales;
    private final AccountPaymentRepository payments;
    private final AgeingService ageing;
    private final PaymentAllocationService allocationService;
    private final Clock clock;

    public AccountController(ContractorAccountRepository accounts, AuthorisedBuyerRepository buyers,
                             JobSiteRepository jobs, SaleRepository sales,
                             AccountPaymentRepository payments, AgeingService ageing,
                             PaymentAllocationService allocationService, Clock clock) {
        this.accounts = accounts;
        this.buyers = buyers;
        this.jobs = jobs;
        this.sales = sales;
        this.payments = payments;
        this.ageing = ageing;
        this.allocationService = allocationService;
        this.clock = clock;
    }

    public record AgeingView(BigDecimal current, BigDecimal days30, BigDecimal days60,
                             BigDecimal days90Plus, BigDecimal total) {

        static AgeingView of(AgeingService.Ageing a) {
            return new AgeingView(a.current(), a.days30(), a.days60(), a.days90Plus(), a.total());
        }
    }

    public record AccountSummary(String code, String name, String contactName, String phone,
                                 String email, String vatNo, String priceTier, BigDecimal creditLimit,
                                 int termsDays, ContractorAccount.Status status, BigDecimal balance,
                                 BigDecimal available, AgeingView ageing, String standing) {
    }

    public record BuyerView(Long id, String name, String roleTitle, BigDecimal perSaleLimit) {
    }

    public record JobView(String code, String description, BigDecimal spend) {
    }

    public record InvoiceView(String documentNo, LocalDate date, String job, String buyer,
                              BigDecimal total, BigDecimal outstanding, boolean overridden) {
    }

    public record PaymentView(String paymentNo, LocalDate date, BigDecimal amount, String method,
                              String reference) {
    }

    public record AccountDetail(AccountSummary summary, List<BuyerView> buyers, List<JobView> jobs,
                                List<InvoiceView> invoices, List<PaymentView> payments) {
    }

    @GetMapping
    public List<AccountSummary> list() {
        return accounts.findAllByOrderByNameAsc().stream().map(this::summarise).toList();
    }

    @GetMapping("/{code}")
    public AccountDetail detail(@PathVariable String code) {
        ContractorAccount account = account(code);
        AgeingService.AccountPosition position = ageing.positionOf(account);

        Map<String, BigDecimal> outstanding = new HashMap<>();
        position.openInvoices().forEach(i -> outstanding.put(i.documentNo(), i.outstanding()));

        Map<Long, String> buyerNames = new HashMap<>();
        List<BuyerView> buyerViews = buyers.findByAccountIdAndActiveTrueOrderByNameAsc(account.getId())
                .stream()
                .peek(b -> buyerNames.put(b.getId(), b.getName()))
                .map(b -> new BuyerView(b.getId(), b.getName(), b.getRoleTitle(), b.getPerSaleLimit()))
                .toList();

        List<Sale> accountSales = sales.findByAccountIdOrderByOccurredAtAsc(account.getId());
        Map<Long, BigDecimal> spendByJob = new HashMap<>();
        accountSales.forEach(s -> spendByJob.merge(s.getJobId(), s.getTotalInclVat(), BigDecimal::add));

        List<JobView> jobViews = jobs.findByAccountIdAndActiveTrueOrderByCodeAsc(account.getId()).stream()
                .map(j -> new JobView(j.getCode(), j.getDescription(),
                        Money.scaled(spendByJob.getOrDefault(j.getId(), BigDecimal.ZERO))))
                .toList();

        Map<Long, String> jobNames = new HashMap<>();
        jobs.findByAccountIdAndActiveTrueOrderByCodeAsc(account.getId())
                .forEach(j -> jobNames.put(j.getId(), j.getDescription()));

        List<InvoiceView> invoiceViews = accountSales.reversed().stream()
                .map(s -> new InvoiceView(s.getDocumentNo(),
                        s.getOccurredAt().atZone(STORE_ZONE).toLocalDate(),
                        jobNames.get(s.getJobId()), buyerNames.get(s.getBuyerId()),
                        s.getTotalInclVat(),
                        outstanding.getOrDefault(s.getDocumentNo(), Money.ZERO),
                        s.getOverrideReason() != null))
                .toList();

        List<PaymentView> paymentViews = payments.findByAccountIdOrderByPaymentDateAsc(account.getId())
                .reversed().stream()
                .map(p -> new PaymentView(p.getPaymentNo(), p.getPaymentDate(), p.getAmount(),
                        p.getMethod().name(), p.getReference()))
                .toList();

        return new AccountDetail(summarise(account), buyerViews, jobViews, invoiceViews, paymentViews);
    }

    /** Everything a statement prints: opening balance, movements, closing ageing. */
    public record StatementLine(LocalDate date, String reference, String detail,
                                BigDecimal debit, BigDecimal credit, BigDecimal balance) {
    }

    public record Statement(AccountSummary account, LocalDate from, LocalDate to,
                            BigDecimal openingBalance, List<StatementLine> lines, AgeingView ageing) {
    }

    @GetMapping("/{code}/statement")
    public Statement statement(@PathVariable String code,
                               @RequestParam(defaultValue = "90") int days) {
        ContractorAccount account = account(code);
        LocalDate to = LocalDate.now(clock.withZone(STORE_ZONE));
        LocalDate from = to.minusDays(days);

        record Movement(LocalDate date, String reference, String detail, BigDecimal debit,
                        BigDecimal credit) {
        }

        List<Movement> movements = new ArrayList<>();
        for (Sale sale : sales.findByAccountIdOrderByOccurredAtAsc(account.getId())) {
            movements.add(new Movement(sale.getOccurredAt().atZone(STORE_ZONE).toLocalDate(),
                    sale.getDocumentNo(), "Invoice", sale.getTotalInclVat(), Money.ZERO));
        }
        for (AccountPayment payment : payments.findByAccountIdOrderByPaymentDateAsc(account.getId())) {
            movements.add(new Movement(payment.getPaymentDate(), payment.getPaymentNo(),
                    "Payment received, " + payment.getMethod().name().toLowerCase(Locale.ROOT),
                    Money.ZERO, payment.getAmount()));
        }
        movements.sort(Comparator.comparing(Movement::date)
                .thenComparing(m -> m.debit().signum() > 0 ? 0 : 1));

        BigDecimal opening = Money.ZERO;
        List<StatementLine> lines = new ArrayList<>();
        BigDecimal running = Money.ZERO;
        for (Movement movement : movements) {
            BigDecimal delta = movement.debit().subtract(movement.credit());
            if (movement.date().isBefore(from)) {
                opening = Money.scaled(opening.add(delta));
                running = opening;
                continue;
            }
            running = Money.scaled(running.add(delta));
            lines.add(new StatementLine(movement.date(), movement.reference(), movement.detail(),
                    movement.debit(), movement.credit(), running));
        }

        return new Statement(summarise(account), from, to, opening, lines,
                AgeingView.of(ageing.positionOf(account).ageing()));
    }

    @PostMapping("/{code}/payments")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public PaymentAllocationService.Receipt recordPayment(@PathVariable String code,
                                                          @Valid @RequestBody SaleRequests.PaymentRequest request,
                                                          @AuthenticationPrincipal UserDetails user) {
        ContractorAccount account = account(code);
        return allocationService.record(account, request.amount(), request.method(),
                request.reference(), user.getUsername(), LocalDate.now(clock.withZone(STORE_ZONE)));
    }

    private ContractorAccount account(String code) {
        return accounts.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("No account " + code));
    }

    private AccountSummary summarise(ContractorAccount account) {
        AgeingService.AccountPosition position = ageing.positionOf(account);
        return new AccountSummary(account.getCode(), account.getName(), account.getContactName(),
                account.getPhone(), account.getEmail(), account.getVatNo(),
                account.getPriceTier().getCode(), account.getCreditLimit(), account.getTermsDays(),
                account.getStatus(), position.balance(), position.available(),
                AgeingView.of(position.ageing()), standing(account, position));
    }

    /** The one-word verdict the counter shows next to the account name. */
    private String standing(ContractorAccount account, AgeingService.AccountPosition position) {
        if (account.getStatus() != ContractorAccount.Status.ACTIVE) {
            return "On hold";
        }
        if (position.balance().compareTo(account.getCreditLimit()) > 0) {
            return "Over limit";
        }
        if (Money.isPositive(position.ageing().days90Plus())) {
            return "90+ days";
        }
        if (position.balance().compareTo(
                account.getCreditLimit().multiply(new BigDecimal("0.85"))) > 0) {
            return "Near limit";
        }
        return "Good standing";
    }
}
