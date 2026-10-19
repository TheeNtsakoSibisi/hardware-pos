package za.co.ntsumo.pos.accounts;

import org.springframework.stereotype.Service;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.payments.PaymentAllocationRepository;
import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Works out what an account owes and how old the debt is.
 *
 * <p>Ageing is calculated from allocations rather than from a running balance
 * column: each invoice knows what has been settled against it, so the buckets
 * always reconcile with the statement. Buckets are by invoice date, which is how
 * a contractor reads their own statement, not by due date.
 */
@Service
public class AgeingService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Africa/Johannesburg");

    private final SaleRepository sales;
    private final PaymentAllocationRepository allocations;
    private final Clock clock;

    public AgeingService(SaleRepository sales, PaymentAllocationRepository allocations, Clock clock) {
        this.sales = sales;
        this.allocations = allocations;
        this.clock = clock;
    }

    /** One invoice and how much of it is still unpaid. */
    public record OpenInvoice(Long saleId, String documentNo, LocalDate date, BigDecimal total,
                              BigDecimal outstanding, long daysOld) {
    }

    /** The four buckets a statement prints, plus the total they add up to. */
    public record Ageing(BigDecimal current, BigDecimal days30, BigDecimal days60, BigDecimal days90Plus) {

        public static final Ageing EMPTY =
                new Ageing(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);

        public BigDecimal total() {
            return Money.scaled(current.add(days30).add(days60).add(days90Plus));
        }

        /** What is overdue past 60 days: the figure the owner chases first. */
        public BigDecimal over60() {
            return Money.scaled(days60.add(days90Plus));
        }
    }

    public record AccountPosition(ContractorAccount account, BigDecimal balance, BigDecimal available,
                                  Ageing ageing, List<OpenInvoice> openInvoices) {
    }

    public AccountPosition positionOf(ContractorAccount account) {
        List<Sale> invoices = sales.findByAccountIdOrderByOccurredAtAsc(account.getId());
        Map<Long, BigDecimal> settled = settledByInvoice(invoices);

        LocalDate today = LocalDate.now(clock.withZone(STORE_ZONE));
        List<OpenInvoice> open = new ArrayList<>();
        BigDecimal balance = Money.ZERO;

        for (Sale invoice : invoices) {
            BigDecimal paid = settled.getOrDefault(invoice.getId(), Money.ZERO);
            BigDecimal outstanding = Money.scaled(invoice.getTotalInclVat().subtract(paid));
            if (outstanding.signum() <= 0) {
                continue;
            }
            balance = balance.add(outstanding);
            LocalDate invoiceDate = invoice.getOccurredAt().atZone(STORE_ZONE).toLocalDate();
            open.add(new OpenInvoice(invoice.getId(), invoice.getDocumentNo(), invoiceDate,
                    invoice.getTotalInclVat(), outstanding,
                    java.time.temporal.ChronoUnit.DAYS.between(invoiceDate, today)));
        }

        balance = Money.scaled(balance);
        return new AccountPosition(account, balance,
                Money.scaled(account.getCreditLimit().subtract(balance)),
                bucket(open), List.copyOf(open));
    }

    private Map<Long, BigDecimal> settledByInvoice(List<Sale> invoices) {
        if (invoices.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = invoices.stream().map(Sale::getId).toList();
        return allocations.sumBySaleIds(ids).stream()
                .collect(Collectors.toMap(
                        PaymentAllocationRepository.AllocatedAmount::getSaleId,
                        row -> Money.scaled(row.getAmount())));
    }

    private Ageing bucket(List<OpenInvoice> open) {
        BigDecimal current = Money.ZERO;
        BigDecimal d30 = Money.ZERO;
        BigDecimal d60 = Money.ZERO;
        BigDecimal d90 = Money.ZERO;
        for (OpenInvoice invoice : open) {
            long age = invoice.daysOld();
            if (age < 30) {
                current = current.add(invoice.outstanding());
            } else if (age < 60) {
                d30 = d30.add(invoice.outstanding());
            } else if (age < 90) {
                d60 = d60.add(invoice.outstanding());
            } else {
                d90 = d90.add(invoice.outstanding());
            }
        }
        return new Ageing(Money.scaled(current), Money.scaled(d30), Money.scaled(d60), Money.scaled(d90));
    }
}
