package za.co.ntsumo.pos.accounts;

import jakarta.persistence.*;

/** A site the contractor is working on. Every account sale is tagged to one. */
@Entity
@Table(name = "job_site")
public class JobSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected JobSite() {
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
