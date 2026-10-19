package za.co.ntsumo.pos.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_line")
public class SaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal qty;

    @Column(nullable = false)
    private String unit;

    /** Price actually charged: shelf price for walk-ins, trade price on account. */
    @Column(name = "unit_price_incl_vat", nullable = false)
    private BigDecimal unitPriceInclVat;

    @Column(name = "line_total_incl_vat", nullable = false)
    private BigDecimal lineTotalInclVat;

    protected SaleLine() {
    }

    public SaleLine(int lineNo, Long productId, String description, BigDecimal qty, String unit,
                    BigDecimal unitPriceInclVat, BigDecimal lineTotalInclVat) {
        this.lineNo = lineNo;
        this.productId = productId;
        this.description = description;
        this.qty = qty;
        this.unit = unit;
        this.unitPriceInclVat = unitPriceInclVat;
        this.lineTotalInclVat = lineTotalInclVat;
    }

    void attachTo(Sale sale) {
        this.sale = sale;
    }

    public Long getId() {
        return id;
    }

    public int getLineNo() {
        return lineNo;
    }

    public Long getProductId() {
        return productId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPriceInclVat() {
        return unitPriceInclVat;
    }

    public BigDecimal getLineTotalInclVat() {
        return lineTotalInclVat;
    }
}
