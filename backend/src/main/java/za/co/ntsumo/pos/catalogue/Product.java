package za.co.ntsumo.pos.catalogue;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A line the store sells. Some have barcodes; many, like loose screws, do not. */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    private String barcode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    /** EACH, BAG, M, KG, M3, LENGTH and so on. */
    @Column(nullable = false)
    private String unit;

    /** True where the item is sold in fractions: cable by the metre, sand by the m3. */
    @Column(name = "decimal_qty", nullable = false)
    private boolean decimalQty;

    @Column(name = "retail_incl_vat", nullable = false)
    private BigDecimal retailInclVat;

    @Column(name = "cost_excl_vat", nullable = false)
    private BigDecimal costExclVat;

    @Column(name = "reorder_level", nullable = false)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean serialised;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isDecimalQty() {
        return decimalQty;
    }

    public BigDecimal getRetailInclVat() {
        return retailInclVat;
    }

    public BigDecimal getCostExclVat() {
        return costExclVat;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public boolean isSerialised() {
        return serialised;
    }

    public boolean isActive() {
        return active;
    }
}
