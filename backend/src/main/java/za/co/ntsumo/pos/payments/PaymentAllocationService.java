package za.co.ntsumo.pos.payments;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ntsumo.pos.accounts.AgeingService;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.sales.DocumentNumberService;
import za.co.ntsumo.pos.sales.TenderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records a payment and decides which invoices it settles.
 *
 * <p>Oldest invoice first. Contractors pay round amounts against a statement
 * rather than invoice by invoice, so the store has to decide the split, and
 * oldest-first is both what accountants expect and what keeps the ageing
 * buckets honest.
 *
 * <p>An overpayment leaves an unallocated remainder on the account rather than
 * being forced onto an invoice that does not exist yet.
 */
@Service
public class PaymentAllocationService {

    private final AccountPaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final AgeingService ageing;
    private final DocumentNumberService documentNumbers;

    public PaymentAllocationService(AccountPaymentRepository payments,
                                    PaymentAllocationRepository allocations,
                                    AgeingService ageing,
                                    DocumentNumberService documentNumbers) {
        this.payments = payments;
        this.allocations = allocations;
        this.ageing = ageing;
        this.documentNumbers = documentNumbers;
    }

    public record Allocation(Long saleId, String documentNo, BigDecimal amount) {
    }

    public record Receipt(AccountPayment payment, List<Allocation> allocated, BigDecimal unallocated) {
    }

    @Transactional
    public Receipt record(ContractorAccount account, BigDecimal amount, TenderType method,
                          String reference, String capturedBy, LocalDate paymentDate) {
        if (!Money.isPositive(amount)) {
            throw new IllegalArgumentException("A payment must be greater than zero");
        }
        BigDecimal received = Money.scaled(amount);
        String paymentNo = documentNumbers.nextPaymentNumber("T1");

        AccountPayment payment = payments.save(new AccountPayment(
                paymentNo, account.getId(), paymentDate, received, method, reference, capturedBy));

        BigDecimal pool = received;
        List<Allocation> applied = new ArrayList<>();
        for (AgeingService.OpenInvoice invoice : ageing.positionOf(account).openInvoices()) {
            if (!Money.isPositive(pool)) {
                break;
            }
            BigDecimal take = Money.min(pool, invoice.outstanding());
            allocations.save(new PaymentAllocation(payment.getId(), invoice.saleId(), take));
            applied.add(new Allocation(invoice.saleId(), invoice.documentNo(), take));
            pool = Money.scaled(pool.subtract(take));
        }

        return new Receipt(payment, List.copyOf(applied), pool);
    }
}
