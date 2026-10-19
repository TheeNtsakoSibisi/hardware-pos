package za.co.ntsumo.pos.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import za.co.ntsumo.pos.payments.PaymentAllocationRepository;
import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleRepository;
import za.co.ntsumo.pos.testsupport.TestData;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The buckets on a statement decide which contractor gets chased this week, so
 * the boundaries are pinned down here rather than discovered at month-end.
 */
@DisplayName("Ageing")
class AgeingServiceTest {

    private static final ZoneId JOHANNESBURG = ZoneId.of("Africa/Johannesburg");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    private SaleRepository sales;
    private PaymentAllocationRepository allocations;
    private AgeingService ageing;
    private ContractorAccount account;

    @BeforeEach
    void setUp() {
        sales = mock(SaleRepository.class);
        allocations = mock(PaymentAllocationRepository.class);
        Clock clock = Clock.fixed(TODAY.atTime(10, 0).atZone(JOHANNESBURG).toInstant(), ZoneOffset.UTC);
        ageing = new AgeingService(sales, allocations, clock);
        account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");
        when(allocations.sumBySaleIds(any())).thenReturn(List.of());
    }

    private Sale invoiceDaysOld(long id, int daysOld, String total) {
        return TestData.accountInvoice(id, "T1-A%05d".formatted(id), account.getId(),
                TODAY.minusDays(daysOld).atTime(9, 0).atZone(JOHANNESBURG).toInstant(), total);
    }

    @Nested
    @DisplayName("bucket boundaries")
    class Boundaries {

        @Test
        @DisplayName("an invoice 29 days old is still current")
        void dayTwentyNine() {
            when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                    .thenReturn(List.of(invoiceDaysOld(1, 29, "1000.00")));

            assertThat(ageing.positionOf(account).ageing().current()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("on day 30 it moves into the 30-day bucket")
        void dayThirty() {
            when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                    .thenReturn(List.of(invoiceDaysOld(1, 30, "1000.00")));

            AgeingService.Ageing result = ageing.positionOf(account).ageing();
            assertThat(result.current()).isEqualByComparingTo("0.00");
            assertThat(result.days30()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("on day 90 it becomes the debt that blocks further credit")
        void dayNinety() {
            when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                    .thenReturn(List.of(invoiceDaysOld(1, 89, "500.00"), invoiceDaysOld(2, 90, "700.00")));

            AgeingService.Ageing result = ageing.positionOf(account).ageing();
            assertThat(result.days60()).isEqualByComparingTo("500.00");
            assertThat(result.days90Plus()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("over 60 days adds the 60 and 90+ buckets together")
        void overSixty() {
            when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                    .thenReturn(List.of(invoiceDaysOld(1, 10, "100.00"), invoiceDaysOld(2, 65, "200.00"),
                            invoiceDaysOld(3, 120, "300.00")));

            assertThat(ageing.positionOf(account).ageing().over60()).isEqualByComparingTo("500.00");
        }
    }

    @Test
    @DisplayName("a part-paid invoice ages only by what is still owed")
    void partPaidInvoice() {
        Sale invoice = invoiceDaysOld(1, 100, "5000.00");
        when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId())).thenReturn(List.of(invoice));
        when(allocations.sumBySaleIds(List.of(1L)))
                .thenReturn(List.of(TestData.allocated(1L, "2000.00")));

        AgeingService.AccountPosition position = ageing.positionOf(account);
        assertThat(position.ageing().days90Plus()).isEqualByComparingTo("3000.00");
        assertThat(position.balance()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("a fully settled invoice drops off the statement")
    void settledInvoiceIgnored() {
        when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                .thenReturn(List.of(invoiceDaysOld(1, 40, "1500.00")));
        when(allocations.sumBySaleIds(List.of(1L)))
                .thenReturn(List.of(TestData.allocated(1L, "1500.00")));

        AgeingService.AccountPosition position = ageing.positionOf(account);
        assertThat(position.balance()).isEqualByComparingTo("0.00");
        assertThat(position.openInvoices()).isEmpty();
    }

    @Test
    @DisplayName("available credit is the limit less what is owed")
    void availableCredit() {
        when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                .thenReturn(List.of(invoiceDaysOld(1, 5, "12500.00")));

        assertThat(ageing.positionOf(account).available()).isEqualByComparingTo("47500.00");
    }

    @Test
    @DisplayName("an account over its limit reports negative available credit")
    void overLimitIsNegative() {
        ContractorAccount small = TestData.account("SIT001", "Sithole Construction", "30000.00", "B");
        when(sales.findByAccountIdOrderByOccurredAtAsc(small.getId()))
                .thenReturn(List.of(invoiceDaysOld(1, 5, "33150.00")));

        assertThat(ageing.positionOf(small).available()).isEqualByComparingTo("-3150.00");
    }

    @Test
    @DisplayName("an account with nothing on it ages to zero, not to an error")
    void emptyAccount() {
        when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId())).thenReturn(List.of());

        AgeingService.AccountPosition position = ageing.positionOf(account);
        assertThat(position.balance()).isEqualByComparingTo("0.00");
        assertThat(position.ageing().total()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("the buckets add up to the balance")
    void bucketsReconcile() {
        when(sales.findByAccountIdOrderByOccurredAtAsc(account.getId()))
                .thenReturn(List.of(invoiceDaysOld(1, 3, "1111.11"), invoiceDaysOld(2, 45, "2222.22"),
                        invoiceDaysOld(3, 75, "3333.33"), invoiceDaysOld(4, 200, "4444.44")));

        AgeingService.AccountPosition position = ageing.positionOf(account);
        assertThat(position.ageing().total()).isEqualByComparingTo(position.balance());
        assertThat(position.balance()).isEqualByComparingTo(new BigDecimal("11111.10"));
    }
}
