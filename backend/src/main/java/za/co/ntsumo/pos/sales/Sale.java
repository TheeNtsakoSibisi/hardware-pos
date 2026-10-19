package za.co.ntsumo.pos.sales;

import jakarta.persistence.*;
import za.co.ntsumo.pos.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One completed sale. Walk-in and account sales are the same record: the
 * difference is how it was settled, not what was sold.
 *
 * <p>A sale is never edited after capture. Fixing one means capturing another
 * that reverses it, which is what makes the offline queue safe to replay and the
 * day's takings reconstructable from the rows alone.
 */
@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Issued by the till, before the server ever sees the sale. Replaying a
     * queued sale after a network drop finds this row instead of double-charging.
     */
    @Column(name = "client_uuid", nullable = false, unique = true)
    private UUID clientUuid;

    /** Per-till human reference, e.g. T1-000123 or T1-A00007. */
    @Column(name = "document_no", nullable = false, unique = true)
    private String documentNo;

    @Column(name = "till_id", nullable = false)
    private String tillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false)
    private SaleType saleType;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "job_id")
    private Long jobId;

    /** The builder's own order number, so their office can match the invoice. */
    @Column(name = "order_ref")
    private String orderRef;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_vat_no")
    private String customerVatNo;

    @Column(name = "total_incl_vat", nullable = false)
    private BigDecimal totalInclVat;

    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Set when a manager approved a sale the credit rules had blocked. */
    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "override_by")
    private String overrideBy;

    @Column(name = "captured_by", nullable = false)
    private String capturedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo")
    private List<SaleLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SaleTender> tenders = new ArrayList<>();

    protected Sale() {
    }

    private Sale(UUID clientUuid, String documentNo, String tillId, SaleType saleType,
                 String capturedBy, Instant occurredAt, List<SaleLine> lines) {
        this.clientUuid = clientUuid;
        this.documentNo = documentNo;
        this.tillId = tillId;
        this.saleType = saleType;
        this.capturedBy = capturedBy;
        this.occurredAt = occurredAt;
        lines.forEach(this::addLine);
        this.totalInclVat = computeTotal();
        this.vatAmount = Money.vatOfInclusive(this.totalInclVat);
    }

    public static Sale walkIn(UUID clientUuid, String documentNo, String tillId, String capturedBy,
                              Instant occurredAt, List<SaleLine> lines, List<SaleTender> tenders,
                              String customerName, String customerVatNo) {
        Sale sale = new Sale(clientUuid, documentNo, tillId, SaleType.WALK_IN, capturedBy, occurredAt, lines);
        sale.customerName = customerName;
        sale.customerVatNo = customerVatNo;
        tenders.forEach(sale::addTender);
        return sale;
    }

    public static Sale onAccount(UUID clientUuid, String documentNo, String tillId, String capturedBy,
                                 Instant occurredAt, List<SaleLine> lines, Long accountId, Long buyerId,
                                 Long jobId, String orderRef, LocalDate dueDate) {
        Sale sale = new Sale(clientUuid, documentNo, tillId, SaleType.ACCOUNT, capturedBy, occurredAt, lines);
        sale.accountId = accountId;
        sale.buyerId = buyerId;
        sale.jobId = jobId;
        sale.orderRef = orderRef;
        sale.dueDate = dueDate;
        return sale;
    }

    public void recordOverride(String approvedBy, String reason) {
        this.overrideBy = approvedBy;
        this.overrideReason = reason;
    }

    private void addLine(SaleLine line) {
        line.attachTo(this);
        lines.add(line);
    }

    private void addTender(SaleTender tender) {
        tender.attachTo(this);
        tenders.add(tender);
    }

    private BigDecimal computeTotal() {
        return Money.scaled(lines.stream()
                .map(SaleLine::getLineTotalInclVat)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Sum of what the customer handed over; zero for an account sale. */
    public BigDecimal tenderedTotal() {
        return Money.scaled(tenders.stream()
                .map(SaleTender::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public Long getId() {
        return id;
    }

    public UUID getClientUuid() {
        return clientUuid;
    }

    public String getDocumentNo() {
        return documentNo;
    }

    public String getTillId() {
        return tillId;
    }

    public SaleType getSaleType() {
        return saleType;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerVatNo() {
        return customerVatNo;
    }

    public BigDecimal getTotalInclVat() {
        return totalInclVat;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public String getOverrideBy() {
        return overrideBy;
    }

    public String getCapturedBy() {
        return capturedBy;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public List<SaleLine> getLines() {
        return List.copyOf(lines);
    }

    public List<SaleTender> getTenders() {
        return List.copyOf(tenders);
    }
}
