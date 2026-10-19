package za.co.ntsumo.pos.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("rounds half a cent up, the way a till does")
    void roundsHalfUp() {
        assertThat(Money.scaled(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(Money.scaled(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("keeps two decimals even when the input has none")
    void alwaysTwoDecimals() {
        assertThat(Money.scaled(new BigDecimal("7")).toPlainString()).isEqualTo("7.00");
    }

    @ParameterizedTest(name = "VAT in R{0} is R{1}")
    @CsvSource({
            "115.00, 15.00",
            "108.99, 14.22",
            "1.00, 0.13",
            "0.00, 0.00"
    })
    @DisplayName("takes VAT out of a VAT-inclusive total")
    void vatOfInclusive(String total, String expectedVat) {
        assertThat(Money.vatOfInclusive(new BigDecimal(total))).isEqualByComparingTo(expectedVat);
    }

    @Test
    @DisplayName("VAT plus the amount excluding VAT adds back to the total")
    void vatReconciles() {
        BigDecimal total = new BigDecimal("8331.51");
        assertThat(Money.exclusiveOf(total).add(Money.vatOfInclusive(total)))
                .isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("multiplies a decimal quantity and rounds once, at the end")
    void lineTotalRoundsOnce() {
        // 12.5 metres of cable at R14.49: 181.125, which must land on 181.13,
        // not on a figure built from an already-rounded unit price.
        assertThat(Money.lineTotal(new BigDecimal("14.49"), new BigDecimal("12.5")))
                .isEqualByComparingTo("181.13");
    }

    @ParameterizedTest(name = "R{0} less {1}% is R{2}")
    @CsvSource({
            "108.99, 10.00, 98.09",
            "108.99, 15.00, 92.64",
            "100.00, 0.00, 100.00"
    })
    @DisplayName("applies a trade discount to the shelf price")
    void appliesDiscount(String price, String percent, String expected) {
        assertThat(Money.applyDiscount(new BigDecimal(price), new BigDecimal(percent)))
                .isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("treats zero and null as not positive")
    void positiveCheck() {
        assertThat(Money.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(Money.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(Money.isPositive(null)).isFalse();
    }
}
