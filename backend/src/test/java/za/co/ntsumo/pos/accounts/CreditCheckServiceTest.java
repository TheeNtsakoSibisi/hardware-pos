package za.co.ntsumo.pos.accounts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.testsupport.TestData;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * These rules decide whether a builder drives away with the materials, so each
 * one is pinned down on its own, and the wording the cashier reads out is
 * checked too.
 */
@DisplayName("Credit check")
class CreditCheckServiceTest {

    private AgeingService ageing;
    private CreditCheckService creditCheck;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        ageing = mock(AgeingService.class);
        creditCheck = new CreditCheckService(ageing, encoder);
    }

    private void positionIs(ContractorAccount account, String balance, String days90Plus) {
        AgeingService.Ageing buckets = new AgeingService.Ageing(
                Money.of(balance).subtract(Money.of(days90Plus)), Money.ZERO, Money.ZERO,
                Money.of(days90Plus));
        when(ageing.positionOf(any())).thenReturn(new AgeingService.AccountPosition(account,
                Money.of(balance), Money.scaled(account.getCreditLimit().subtract(Money.of(balance))),
                buckets, List.of()));
    }

    @Test
    @DisplayName("approves a sale inside the limit on a clean account")
    void approvesHealthyAccount() {
        ContractorAccount account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");
        positionIs(account, "27926.45", "0.00");
        AuthorisedBuyer buyer = TestData.buyer("Thabo Mokoena", account.getId(), null, "x");

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, new BigDecimal("4500.00"));

        assertThat(decision.approved()).isTrue();
        assertThat(decision.refusals()).isEmpty();
    }

    @Test
    @DisplayName("refuses when the sale would cross the credit limit, and says by how much")
    void refusesOverLimit() {
        ContractorAccount account = TestData.account("SIT001", "Sithole Construction", "30000.00", "B");
        positionIs(account, "29800.00", "0.00");
        AuthorisedBuyer buyer = TestData.buyer("Mandla Nkosi", account.getId(), null, "x");

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, new BigDecimal("500.00"));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.refusals()).singleElement().satisfies(refusal -> {
            assertThat(refusal.reason()).isEqualTo(CreditCheckService.Reason.OVER_CREDIT_LIMIT);
            assertThat(refusal.message()).contains("R30300.00", "over the R30000.00 limit by R300.00");
        });
    }

    @Test
    @DisplayName("allows a sale that lands exactly on the limit")
    void exactlyOnLimitIsAllowed() {
        ContractorAccount account = TestData.account("MAH001", "Mahlangu Roofing", "10000.00", "A");
        positionIs(account, "9000.00", "0.00");
        AuthorisedBuyer buyer = TestData.buyer("Nomsa Mahlangu", account.getId(), null, "x");

        assertThat(creditCheck.check(account, buyer, new BigDecimal("1000.00")).approved()).isTrue();
    }

    @Test
    @DisplayName("refuses while anything is more than 90 days old, even well inside the limit")
    void refusesOnNinetyDayDebt() {
        ContractorAccount account = TestData.account("KHU001", "Khumalo Renovations", "25000.00", "A");
        positionIs(account, "15726.44", "15726.44");
        AuthorisedBuyer buyer = TestData.buyer("Bongani Khumalo", account.getId(), null, "x");

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, new BigDecimal("100.00"));

        assertThat(decision.approved()).isFalse();
        assertThat(decision.refusals()).extracting(CreditCheckService.Refusal::reason)
                .containsExactly(CreditCheckService.Reason.OVERDUE_90_DAYS);
    }

    @Test
    @DisplayName("refuses on an account that is on hold")
    void refusesOnHold() {
        ContractorAccount account = TestData.account("KHU001", "Khumalo Renovations", "25000.00", "A",
                ContractorAccount.Status.ON_HOLD);
        positionIs(account, "1000.00", "0.00");
        AuthorisedBuyer buyer = TestData.buyer("Bongani Khumalo", account.getId(), null, "x");

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, new BigDecimal("100.00"));

        assertThat(decision.refusals()).extracting(CreditCheckService.Refusal::message)
                .containsExactly("Khumalo Renovations is on hold.");
    }

    @Test
    @DisplayName("holds a buyer to their own per-sale limit")
    void refusesOverBuyerLimit() {
        ContractorAccount account = TestData.account("VWY001", "Van Wyk Electrical", "40000.00", "A");
        positionIs(account, "1000.00", "0.00");
        AuthorisedBuyer driver = TestData.buyer("Kagiso Molefe", account.getId(), "5000.00", "x");

        CreditCheckService.Decision decision = creditCheck.check(account, driver, new BigDecimal("5000.01"));

        assertThat(decision.refusals()).extracting(CreditCheckService.Refusal::reason)
                .containsExactly(CreditCheckService.Reason.OVER_BUYER_LIMIT);
    }

    @Test
    @DisplayName("a buyer with no limit is not stopped by one")
    void noBuyerLimit() {
        ContractorAccount account = TestData.account("VWY001", "Van Wyk Electrical", "40000.00", "A");
        positionIs(account, "1000.00", "0.00");
        AuthorisedBuyer owner = TestData.buyer("Pieter van Wyk", account.getId(), null, "x");

        assertThat(creditCheck.check(account, owner, new BigDecimal("20000.00")).approved()).isTrue();
    }

    @Test
    @DisplayName("reports every reason at once, so the cashier explains it once")
    void collectsAllReasons() {
        ContractorAccount account = TestData.account("KHU001", "Khumalo Renovations", "25000.00", "A",
                ContractorAccount.Status.ON_HOLD);
        positionIs(account, "24900.00", "15726.44");
        AuthorisedBuyer buyer = TestData.buyer("Site hand", account.getId(), "500.00", "x");

        CreditCheckService.Decision decision = creditCheck.check(account, buyer, new BigDecimal("1000.00"));

        assertThat(decision.refusals()).extracting(CreditCheckService.Refusal::reason)
                .containsExactly(CreditCheckService.Reason.ACCOUNT_ON_HOLD,
                        CreditCheckService.Reason.OVERDUE_90_DAYS,
                        CreditCheckService.Reason.OVER_CREDIT_LIMIT,
                        CreditCheckService.Reason.OVER_BUYER_LIMIT);
        assertThat(decision.requiresOverride()).isTrue();
    }

    @Test
    @DisplayName("accepts the buyer's PIN and rejects a wrong one")
    void checksPin() {
        ContractorAccount account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");
        AuthorisedBuyer buyer = TestData.buyer("Jabu Dlamini", account.getId(), null, encoder.encode("2201"));

        assertThat(creditCheck.pinMatches(buyer, "2201")).isTrue();
        assertThat(creditCheck.pinMatches(buyer, "2202")).isFalse();
        assertThat(creditCheck.pinMatches(buyer, null)).isFalse();
    }
}
