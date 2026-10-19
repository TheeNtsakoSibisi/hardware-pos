package za.co.ntsumo.pos.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Every amount in this system is rands, to two decimals, rounded half-up.
 *
 * <p>There is no {@code double} anywhere in the money path. A cent lost to binary
 * floating point is a cent the store cannot reconcile at cash-up, and a contractor
 * statement that is out by a cent is a statement the contractor will not pay.
 */
public final class Money {

    public static final BigDecimal ZERO = scaled(BigDecimal.ZERO);

    /** South African VAT rate. Prices in this system are VAT-inclusive. */
    public static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    private static final BigDecimal VAT_DIVISOR = BigDecimal.ONE.add(VAT_RATE);

    private Money() {
    }

    /** Rounds to the nearest cent, half-up, as a till does. */
    public static BigDecimal scaled(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String amount) {
        return scaled(new BigDecimal(amount));
    }

    /** Line total for a quantity at a unit price, rounded once at the end. */
    public static BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal quantity) {
        return scaled(unitPrice.multiply(quantity));
    }

    /**
     * The VAT contained in a VAT-inclusive amount: {@code total - total / 1.15}.
     *
     * <p>Deriving the VAT from the total this way, rather than adding VAT to a
     * net figure, keeps the printed total equal to what the customer paid.
     */
    public static BigDecimal vatOfInclusive(BigDecimal inclusiveTotal) {
        return scaled(inclusiveTotal.subtract(exclusiveOf(inclusiveTotal)));
    }

    /** The amount excluding VAT, given a VAT-inclusive amount. */
    public static BigDecimal exclusiveOf(BigDecimal inclusiveTotal) {
        return inclusiveTotal.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    /** Applies a trade discount percentage to a shelf price. */
    public static BigDecimal applyDiscount(BigDecimal price, BigDecimal discountPercent) {
        BigDecimal factor = BigDecimal.ONE.subtract(
                discountPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return scaled(price.multiply(factor));
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
