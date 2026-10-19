package za.co.ntsumo.pos.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import za.co.ntsumo.pos.catalogue.Product;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a shopping list using local trade vocabulary and weighted word overlap.
 * No network, no API key, no cost.
 *
 * <p>This is the default so the store can quote without depending on anything
 * external, and so the feature degrades to "good enough" rather than to nothing
 * when the line is down. It handles the common shapes and flags the rest for the
 * cashier, which is the same contract the Claude parser works to.
 *
 * <p>The work is in the tokeniser. A builder writes "6 x 110 pipe" and the
 * catalogue says "PVC sewer pipe 110mm 6m", so sizes have to be compared as
 * numbers rather than as strings, and "lintels" has to reach "lintel".
 */
@Component
@ConditionalOnMissingBean(name = "claudeShoppingListParser")
public class RuleBasedShoppingListParser implements ShoppingListParser {

    /** Trade shorthand, expanded into the words the catalogue actually uses. */
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("pocket", List.of("bag")),
            Map.entry("cube", List.of("m3", "loose")),
            Map.entry("surfix", List.of("surfix", "cable")),
            Map.entry("pva", List.of("pva")),
            Map.entry("db", List.of("db", "board")),
            Map.entry("ibr", List.of("ibr", "sheet")),
            Map.entry("brick", List.of("brick")),
            Map.entry("bend", List.of("bend")),
            Map.entry("pipe", List.of("pipe")),
            Map.entry("screw", List.of("screw")),
            Map.entry("lintel", List.of("lintel")),
            Map.entry("sand", List.of("sand")),
            Map.entry("stone", List.of("stone")));

    /** Words that carry no product meaning and would otherwise drag a match. */
    private static final Set<String> STOP_WORDS = Set.of(
            "of", "the", "and", "for", "off", "job", "site", "please", "pls", "thanks",
            "hi", "hello", "morning", "afternoon", "order", "ref", "need", "want", "get");

    private static final Pattern LEADING_QTY = Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:x\\s+)?(.*)$");
    private static final Pattern TRAILING_QTY = Pattern.compile("^(.*?)\\s+x\\s*(\\d+(?:[.,]\\d+)?)\\s*$");

    private final CatalogueIndex catalogue;

    public RuleBasedShoppingListParser(CatalogueIndex catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public Parsed parse(String shoppingList) {
        List<Product> products = catalogue.all();
        List<ParsedLine> parsed = new ArrayList<>();

        for (String raw : shoppingList.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || !line.matches(".*[a-zA-Z].*")) {
                continue;
            }
            ParsedLine candidate = parseLine(line, products);
            // A line that matched nothing and asked for no quantity is almost
            // always a heading or a greeting ("Mandla - Protea Glen job").
            if (candidate == null) {
                continue;
            }
            parsed.add(candidate);
        }
        return new Parsed(List.copyOf(parsed), "rule-based");
    }

    private ParsedLine parseLine(String line, List<Product> products) {
        BigDecimal qty = BigDecimal.ONE;
        boolean qtyGiven = false;
        String text = line;

        Matcher leading = LEADING_QTY.matcher(line);
        Matcher trailing = TRAILING_QTY.matcher(line);
        if (leading.matches() && !leading.group(2).isBlank()) {
            qty = new BigDecimal(leading.group(1).replace(',', '.'));
            text = leading.group(2);
            qtyGiven = true;
        } else if (trailing.matches()) {
            qty = new BigDecimal(trailing.group(2).replace(',', '.'));
            text = trailing.group(1);
            qtyGiven = true;
        }

        List<String> words = tokens(text);
        if (words.isEmpty()) {
            return null;
        }

        Product best = null;
        double bestScore = 0;
        for (Product product : products) {
            double score = score(words, product);
            if (score > bestScore) {
                bestScore = score;
                best = product;
            }
        }

        if (best == null || bestScore < 0.45) {
            return qtyGiven
                    ? new ParsedLine(line, null, qty, Confidence.LOW,
                    "Nothing in the catalogue matches; choose the product.")
                    : null;
        }

        Confidence confidence;
        String note = "";
        if (bestScore >= 0.8 && qtyGiven) {
            confidence = Confidence.HIGH;
        } else if (bestScore >= 0.6) {
            confidence = Confidence.MEDIUM;
            note = qtyGiven ? "Closest match on the words given; please check."
                    : "No quantity given, assumed one.";
        } else {
            confidence = Confidence.LOW;
            note = "Best guess; please check the product and quantity.";
        }
        if (confidence == Confidence.HIGH && !best.getUnit().equals("EACH")) {
            note = "Priced per %s.".formatted(best.getUnit().toLowerCase(Locale.ROOT));
        }
        return new ParsedLine(line, best.getSku(), qty, confidence, note);
    }

    /**
     * Weighted overlap. A size (110, 2.5, 42.5) is worth more than a common word
     * because it is what distinguishes one pipe from another, and a short query
     * against a long product name is penalised so "pipe" alone does not win.
     */
    private double score(List<String> words, Product product) {
        if (words.isEmpty()) {
            return 0;
        }
        Set<String> productWords = new HashSet<>(tokens(product.getName() + " " + product.getUnit()));
        double matched = 0;
        double possible = 0;
        for (String word : words) {
            double weight = isNumeric(word) ? 2.5 : 1;
            possible += weight;
            if (productWords.contains(word)) {
                matched += weight;
            }
        }
        double coverage = matched / possible;
        double specificity = Math.min(1.0, (double) words.size() / Math.max(1, productWords.size()));
        return coverage * (0.75 + 0.25 * specificity);
    }

    /**
     * Splits on punctuation, then splits letters from digits so "110mm" offers
     * both "110" and "mm", and "4x40" offers "4" and "40". Plurals lose their s.
     */
    private List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String chunk : text.toLowerCase(Locale.ROOT).split("[^a-z0-9.]+")) {
            if (chunk.isBlank()) {
                continue;
            }
            for (String piece : splitLettersAndDigits(chunk)) {
                String token = singular(piece);
                if (token.length() < 2 && !isNumeric(token)) {
                    continue;
                }
                if (STOP_WORDS.contains(token)) {
                    continue;
                }
                out.addAll(SYNONYMS.getOrDefault(token, List.of(token)));
            }
        }
        return out;
    }

    private List<String> splitLettersAndDigits(String chunk) {
        List<String> pieces = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+(?:\\.\\d+)?|[a-z]+").matcher(chunk);
        while (matcher.find()) {
            pieces.add(trimTrailingZero(matcher.group()));
        }
        return pieces.isEmpty() ? List.of(chunk) : pieces;
    }

    /** 2.40 and 2.4 are the same lintel. */
    private String trimTrailingZero(String token) {
        if (!token.contains(".")) {
            return token;
        }
        String trimmed = token.replaceAll("0+$", "");
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /** "bags" to "bag", and "boxes" to "box" rather than to "boxe". */
    private String singular(String token) {
        if (token.length() <= 3 || !token.endsWith("s") || token.endsWith("ss")) {
            return token;
        }
        if (token.endsWith("es")) {
            String stem = token.substring(0, token.length() - 2);
            if (stem.endsWith("x") || stem.endsWith("s") || stem.endsWith("z")
                    || stem.endsWith("ch") || stem.endsWith("sh")) {
                return stem;
            }
        }
        return token.substring(0, token.length() - 1);
    }

    private boolean isNumeric(String token) {
        return !token.isEmpty() && Character.isDigit(token.charAt(0));
    }
}
