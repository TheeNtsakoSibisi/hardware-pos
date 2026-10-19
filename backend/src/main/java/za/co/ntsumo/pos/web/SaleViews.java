package za.co.ntsumo.pos.web;

import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleType;
import za.co.ntsumo.pos.sales.TenderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** What the till prints from: everything on the document, nothing else. */
public final class SaleViews {

    private SaleViews() {
    }

    public record LineView(String description, BigDecimal qty, String unit,
                           BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record TenderView(TenderType type, BigDecimal amount, String reference,
                             BigDecimal cashReceived, BigDecimal changeGiven) {
    }

    public record SaleView(String documentNo, SaleType saleType, Instant occurredAt,
                           BigDecimal total, BigDecimal vat, LocalDate dueDate,
                           String customerName, String customerVatNo, String orderRef,
                           /* Printed on an account invoice: who it is for, who collected, which site. */
                           String accountName, String buyerName, String jobDescription,
                           String overrideReason, String overrideBy, String capturedBy,
                           List<LineView> lines, List<TenderView> tenders) {
    }

    /**
     * The document carries the names, not just the ids, because the printed
     * invoice has to stand on its own when a contractor queries it months later.
     */
    public static SaleView of(Sale sale, String accountName, String buyerName, String jobDescription) {
        return new SaleView(sale.getDocumentNo(), sale.getSaleType(), sale.getOccurredAt(),
                sale.getTotalInclVat(), sale.getVatAmount(), sale.getDueDate(),
                sale.getCustomerName(), sale.getCustomerVatNo(), sale.getOrderRef(),
                accountName, buyerName, jobDescription,
                sale.getOverrideReason(), sale.getOverrideBy(), sale.getCapturedBy(),
                sale.getLines().stream()
                        .map(l -> new LineView(l.getDescription(), l.getQty(), l.getUnit(),
                                l.getUnitPriceInclVat(), l.getLineTotalInclVat()))
                        .toList(),
                sale.getTenders().stream()
                        .map(t -> new TenderView(t.getTenderType(), t.getAmount(), t.getReference(),
                                t.getCashReceived(), t.getChangeGiven()))
                        .toList());
    }
}
