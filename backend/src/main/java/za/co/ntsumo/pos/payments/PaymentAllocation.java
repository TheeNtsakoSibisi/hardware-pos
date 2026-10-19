package za.co.ntsumo.pos.payments;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** How much of one payment settled one invoice. */
@Entity
@Table(name = "payment_allocation")
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(nullable = false)
    private BigDecimal amount;

    protected PaymentAllocation() {
    }

    public PaymentAllocation(Long paymentId, Long saleId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.saleId = saleId;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getSaleId() {
        return saleId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
