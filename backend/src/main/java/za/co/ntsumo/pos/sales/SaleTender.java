package za.co.ntsumo.pos.sales;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_tender")
public class SaleTender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(name = "tender_type", nullable = false)
    private TenderType tenderType;

    @Column(nullable = false)
    private BigDecimal amount;

    /** Card approval code or EFT reference, captured for cash-up. */
    private String reference;

    @Column(name = "cash_received")
    private BigDecimal cashReceived;

    @Column(name = "change_given")
    private BigDecimal changeGiven;

    protected SaleTender() {
    }

    public SaleTender(TenderType tenderType, BigDecimal amount, String reference,
                      BigDecimal cashReceived, BigDecimal changeGiven) {
        this.tenderType = tenderType;
        this.amount = amount;
        this.reference = reference;
        this.cashReceived = cashReceived;
        this.changeGiven = changeGiven;
    }

    void attachTo(Sale sale) {
        this.sale = sale;
    }

    public Long getId() {
        return id;
    }

    public TenderType getTenderType() {
        return tenderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public BigDecimal getCashReceived() {
        return cashReceived;
    }

    public BigDecimal getChangeGiven() {
        return changeGiven;
    }
}
