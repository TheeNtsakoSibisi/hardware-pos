package za.co.ntsumo.pos.catalogue;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Stock is a running total of movements, never a mutable number on the product
 * row. Two tills selling the last bag of cement at the same moment each append a
 * movement instead of overwriting each other's count.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    public enum Reason { SALE, RECEIPT, RETURN, COUNT, WRITE_OFF, OPENING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "qty_delta", nullable = false)
    private BigDecimal qtyDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected StockMovement() {
    }

    public StockMovement(Long productId, BigDecimal qtyDelta, Reason reason, String reference) {
        this.productId = productId;
        this.qtyDelta = qtyDelta;
        this.reason = reason;
        this.reference = reference;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getQtyDelta() {
        return qtyDelta;
    }

    public Reason getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }
}
