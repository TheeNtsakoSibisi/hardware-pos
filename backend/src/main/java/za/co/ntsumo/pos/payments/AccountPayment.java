package za.co.ntsumo.pos.payments;

import jakarta.persistence.*;
import za.co.ntsumo.pos.sales.TenderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Money received from a contractor against their account. */
@Entity
@Table(name = "account_payment")
public class AccountPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, unique = true)
    private String paymentNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderType method;

    private String reference;

    @Column(name = "captured_by", nullable = false)
    private String capturedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AccountPayment() {
    }

    public AccountPayment(String paymentNo, Long accountId, LocalDate paymentDate, BigDecimal amount,
                          TenderType method, String reference, String capturedBy) {
        this.paymentNo = paymentNo;
        this.accountId = accountId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.capturedBy = capturedBy;
    }

    public Long getId() {
        return id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TenderType getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public String getCapturedBy() {
        return capturedBy;
    }
}
