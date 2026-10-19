package za.co.ntsumo.pos.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.accounts.ContractorAccountRepository;
import za.co.ntsumo.pos.ai.ShoppingListParser;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.catalogue.ProductRepository;
import za.co.ntsumo.pos.money.Money;
import za.co.ntsumo.pos.sales.PricingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a builder's list into a priced quote the cashier reviews.
 *
 * <p>Nothing here commits anything: the response is a draft. Lines the parser
 * was unsure about are returned with their confidence so the till can highlight
 * them, and the cashier confirms every line before it becomes a sale.
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final ShoppingListParser parser;
    private final ProductRepository products;
    private final ContractorAccountRepository accounts;
    private final PricingService pricing;

    public QuoteController(ShoppingListParser parser, ProductRepository products,
                           ContractorAccountRepository accounts, PricingService pricing) {
        this.parser = parser;
        this.products = products;
        this.accounts = accounts;
        this.pricing = pricing;
    }

    public record QuoteRequest(
            @NotBlank @Size(max = 4000) String shoppingList,
            /* Null quotes at walk-in prices; an account code quotes at its trade tier. */
            String accountCode) {
    }

    public record QuoteLine(String sourceText, String sku, String description, BigDecimal qty,
                            String unit, BigDecimal unitPrice, BigDecimal lineTotal,
                            ShoppingListParser.Confidence confidence, String note,
                            boolean needsChecking) {
    }

    public record Quote(List<QuoteLine> lines, BigDecimal total, int linesNeedingChecks,
                        String pricedFor, String parserUsed) {
    }

    @PostMapping
    public Quote quote(@Valid @RequestBody QuoteRequest request) {
        ContractorAccount account = request.accountCode() == null || request.accountCode().isBlank()
                ? null
                : accounts.findByCode(request.accountCode()).orElse(null);

        ShoppingListParser.Parsed parsed = parser.parse(request.shoppingList());

        List<QuoteLine> lines = new ArrayList<>();
        BigDecimal total = Money.ZERO;
        int needChecking = 0;

        for (ShoppingListParser.ParsedLine line : parsed.lines()) {
            Product product = line.sku() == null ? null : products.findBySku(line.sku()).orElse(null);
            boolean checkMe = product == null || line.confidence() != ShoppingListParser.Confidence.HIGH;
            if (checkMe) {
                needChecking++;
            }
            if (product == null) {
                lines.add(new QuoteLine(line.sourceText(), null, null, line.qty(), null,
                        null, null, line.confidence(), line.note(), true));
                continue;
            }
            BigDecimal unitPrice = pricing.unitPriceFor(product, account);
            BigDecimal lineTotal = Money.lineTotal(unitPrice, line.qty());
            total = total.add(lineTotal);
            lines.add(new QuoteLine(line.sourceText(), product.getSku(), product.getName(),
                    line.qty(), product.getUnit(), unitPrice, lineTotal,
                    line.confidence(), line.note(), checkMe));
        }

        return new Quote(List.copyOf(lines), Money.scaled(total), needChecking,
                account == null ? "Walk-in retail" : account.getName() + " (tier " + account.getPriceTier().getCode() + ")",
                parsed.parserUsed());
    }
}
