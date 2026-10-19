package za.co.ntsumo.pos.accounts;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A trade discount band. Held as data so the owner can add one without a release. */
@Entity
@Table(name = "price_tier")
public class PriceTier {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent;

    protected PriceTier() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
}
