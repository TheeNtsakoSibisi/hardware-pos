package za.co.ntsumo.pos.accounts;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Someone the account holder has authorised to collect goods: the owner, a site
 * foreman, a driver. Each confirms with a PIN at the counter, so "who signed for
 * this?" has an answer when the invoice is queried.
 */
@Entity
@Table(name = "authorised_buyer")
public class AuthorisedBuyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String name;

    @Column(name = "role_title")
    private String roleTitle;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    /** Null means this buyer has no per-sale ceiling. */
    @Column(name = "per_sale_limit")
    private BigDecimal perSaleLimit;

    @Column(nullable = false)
    private boolean active = true;

    protected AuthorisedBuyer() {
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public String getPinHash() {
        return pinHash;
    }

    public BigDecimal getPerSaleLimit() {
        return perSaleLimit;
    }

    public boolean isActive() {
        return active;
    }
}
