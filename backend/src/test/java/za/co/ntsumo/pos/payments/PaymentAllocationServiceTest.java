package za.co.ntsumo.pos.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.ntsumo.pos.accounts.AgeingService;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.sales.DocumentNumberService;
import za.co.ntsumo.pos.sales.TenderType;
import za.co.ntsumo.pos.testsupport.TestData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Contractors pay a round amount off a statement, so the store decides which
 * invoices that money settles. Getting this wrong quietly corrupts the ageing.
 */
@DisplayName("Payment allocation")
class PaymentAllocationServiceTest {

    private AccountPaymentRepository payments;
    private PaymentAllocationRepository allocations;
    private AgeingService ageing;
    private PaymentAllocationService service;
    private ContractorAccount account;

    @BeforeEach
    void setUp() {
        payments = mock(AccountPaymentRepository.class);
        allocations = mock(PaymentAllocationRepository.class);
        ageing = mock(AgeingService.class);
        DocumentNumberService numbers = mock(DocumentNumberService.class);
        when(numbers.nextPaymentNumber(any())).thenReturn("RCPT-00042");
        when(payments.save(any())).thenAnswer(call -> {
            AccountPayment payment = call.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 7L);
            return payment;
        });
        when(allocations.save(any())).thenAnswer(call -> call.getArgument(0));

        service = new PaymentAllocationService(payments, allocations, ageing, numbers);
        account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");
    }

    private void openInvoices(AgeingService.OpenInvoice... invoices) {
        when(ageing.positionOf(account)).thenReturn(new AgeingService.AccountPosition(
                account, Money.ZERO, Money.ZERO, AgeingService.Ageing.EMPTY, List.of(invoices)));
    }

    private AgeingService.OpenInvoice open(long id, String documentNo, int daysOld, String outstanding) {
        LocalDate date = LocalDate.now().minusDays(daysOld);
        return new AgeingService.OpenInvoice(id, documentNo, date, new BigDecimal(outstanding),
                new BigDecimal(outstanding), daysOld);
    }

    private PaymentAllocationService.Receipt pay(String amount) {
        return service.record(account, new BigDecimal(amount), TenderType.EFT, "MOK001",
                "owner", LocalDate.now());
    }

    @Test
    @DisplayName("settles the oldest invoice first")
    void oldestFirst() {
        openInvoices(open(1, "T1-A00001", 95, "3000.00"), open(2, "T1-A00002", 40, "2000.00"));

        PaymentAllocationService.Receipt receipt = pay("3000.00");

        assertThat(receipt.allocated()).singleElement()
                .satisfies(a -> assertThat(a.documentNo()).isEqualTo("T1-A00001"));
        assertThat(receipt.unallocated()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("spills across invoices when one payment covers several")
    void spillsAcrossInvoices() {
        openInvoices(open(1, "T1-A00001", 95, "3000.00"), open(2, "T1-A00002", 40, "2000.00"),
                open(3, "T1-A00003", 10, "1500.00"));

        PaymentAllocationService.Receipt receipt = pay("5500.00");

        assertThat(receipt.allocated()).extracting(PaymentAllocationService.Allocation::documentNo)
                .containsExactly("T1-A00001", "T1-A00002", "T1-A00003");
        assertThat(receipt.allocated()).extracting(PaymentAllocationService.Allocation::amount)
                .containsExactly(new BigDecimal("3000.00"), new BigDecimal("2000.00"),
                        new BigDecimal("500.00"));
        verify(allocations, times(3)).save(any());
    }

    @Test
    @DisplayName("part-pays the oldest invoice and leaves the rest open")
    void partPayment() {
        openInvoices(open(1, "T1-A00001", 95, "3000.00"));

        assertThat(pay("1200.00").allocated()).singleElement()
                .satisfies(a -> assertThat(a.amount()).isEqualByComparingTo("1200.00"));
    }

    @Test
    @DisplayName("leaves an overpayment sitting on the account rather than forcing it onto an invoice")
    void overpaymentStaysUnallocated() {
        openInvoices(open(1, "T1-A00001", 95, "3000.00"));

        PaymentAllocationService.Receipt receipt = pay("5000.00");

        assertThat(receipt.unallocated()).isEqualByComparingTo("2000.00");
        assertThat(receipt.allocated()).hasSize(1);
    }

    @Test
    @DisplayName("a payment on an account with nothing owing allocates nothing")
    void nothingOwing() {
        openInvoices();

        PaymentAllocationService.Receipt receipt = pay("1000.00");

        assertThat(receipt.allocated()).isEmpty();
        assertThat(receipt.unallocated()).isEqualByComparingTo("1000.00");
        verify(allocations, never()).save(any());
    }

    @Test
    @DisplayName("refuses a payment of zero or less")
    void refusesNonPositive() {
        assertThatThrownBy(() -> pay("0.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        verify(payments, never()).save(any());
    }

    @Test
    @DisplayName("records the payment with its receipt number and method")
    void recordsPayment() {
        openInvoices(open(1, "T1-A00001", 95, "3000.00"));

        PaymentAllocationService.Receipt receipt = pay("3000.00");

        assertThat(receipt.payment().getPaymentNo()).isEqualTo("RCPT-00042");
        assertThat(receipt.payment().getMethod()).isEqualTo(TenderType.EFT);
        assertThat(receipt.payment().getAmount()).isEqualByComparingTo("3000.00");
    }
}
