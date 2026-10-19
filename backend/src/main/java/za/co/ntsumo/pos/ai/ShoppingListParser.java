package za.co.ntsumo.pos.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Turns a builder's shopping list into priced quote lines.
 *
 * <p>Builders send lists as WhatsApp text or a photo of a note: "10 pockets
 * cement, 2 cube sand, 6 x 110 pipe". Reading one by hand at the counter takes
 * twenty minutes while a queue builds.
 *
 * <p>Two implementations sit behind this interface. The rule-based one ships by
 * default and needs nothing external; the Claude one is switched on by setting
 * an API key. The counter does not know or care which is running, and the store
 * is never blocked by an outage of either.
 */
public interface ShoppingListParser {

    /** How sure the parser is, which decides whether the cashier must check the line. */
    enum Confidence { HIGH, MEDIUM, LOW }

    /**
     * @param sourceText the line as the builder wrote it, kept for the cashier to compare
     * @param sku        the matched product, or null when nothing in the catalogue fits
     * @param note       what was assumed, in a few words, when anything was
     */
    record ParsedLine(String sourceText, String sku, BigDecimal qty, Confidence confidence, String note) {
    }

    record Parsed(List<ParsedLine> lines, String parserUsed) {
    }

    Parsed parse(String shoppingList);
}
