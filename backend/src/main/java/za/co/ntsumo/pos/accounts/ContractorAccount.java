package za.co.ntsumo.pos.accounts;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A builder or trade customer who buys on credit and settles monthly. */
@Entity
@Table(name = "contractor_account")
public class ContractorAccount {

    public enum Status { ACTIVE, ON_HOLD, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_name")
    private String contactName;

    private String phone;

    private String email;

    @Column(name = "vat_no")
    private String vatNo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "price_tier", nullable = false)
    private PriceTier priceTier;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "terms_days", nullable = false)
    private int termsDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    protected ContractorAccount() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getVatNo() {
        return vatNo;
    }

    public PriceTier getPriceTier() {
        return priceTier;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public int getTermsDays() {
        return termsDays;
    }

    public Status getStatus() {
        return status;
    }
}
