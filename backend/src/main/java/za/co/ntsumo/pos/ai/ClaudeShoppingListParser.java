package za.co.ntsumo.pos.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses a shopping list with Claude, which handles the vocabulary and the
 * shorthand a rule-based parser cannot: "2 cube sand" as cubic metres, "6 x 110"
 * as six 6m lengths of 110mm pipe, "roof screws" with no quantity at all.
 *
 * <p>Switched on by setting {@code pos.ai.anthropic-api-key}; without it the
 * rule-based parser runs instead and the counter behaves the same way. Anything
 * the model is unsure of comes back flagged rather than silently priced: no
 * quote reaches a customer without a person having looked at it.
 */
@Component("claudeShoppingListParser")
@ConditionalOnProperty(prefix = "pos.ai", name = "anthropic-api-key", matchIfMissing = false)
public class ClaudeShoppingListParser implements ShoppingListParser {

    private static final Logger log = LoggerFactory.getLogger(ClaudeShoppingListParser.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String INSTRUCTIONS = """
            You turn builders' shopping lists into quote lines for a South African hardware store.

            Local vocabulary: "pocket" is a 50kg bag, "cube" is a cubic metre of loose sand or
            stone, "110" means 110mm pipe, "surfix" is flat electrical cable, "pva" is PVA paint.

            For each item the builder asked for, choose the single best match from the CATALOGUE
            below and give the quantity in that product's own unit. Ignore greetings, names and
            job references. Never invent a SKU that is not in the catalogue: use null instead.

            confidence is "HIGH" when the match and the quantity are both clear, "MEDIUM" when you
            assumed something (say what, in under 15 words, in note), and "LOW" when it is a guess.

            Reply with JSON only, in this shape:
            {"lines":[{"sourceText":"10 pockets cement","sku":"BLD-0001","qty":10,"confidence":"HIGH","note":""}]}
            """;

    private final RestClient restClient;
    private final CatalogueIndex catalogue;
    private final ObjectMapper objectMapper;
    private final RuleBasedShoppingListParser fallback;
    private final String model;

    public ClaudeShoppingListParser(CatalogueIndex catalogue, ObjectMapper objectMapper,
                                    AiProperties properties) {
        this.catalogue = catalogue;
        this.objectMapper = objectMapper;
        this.fallback = new RuleBasedShoppingListParser(catalogue);
        this.model = properties.model();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.anthropicApiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Parsed parse(String shoppingList) {
        try {
            String prompt = INSTRUCTIONS
                    + "\nLIST:\n" + shoppingList
                    + "\n\nCATALOGUE:\n" + catalogue.asText();

            JsonNode response = restClient.post()
                    .uri("/v1/messages")
                    .body(Map.of(
                            "model", model,
                            "max_tokens", 2000,
                            "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve()
                    .body(JsonNode.class);

            return new Parsed(readLines(response), "claude:" + model);
        } catch (Exception ex) {
            // A quote is worth more than a perfect quote. If Claude is
            // unreachable, rate limited or slow, the counter still gets lines.
            log.warn("Claude parse failed, falling back to the rule-based parser: {}", ex.getMessage());
            Parsed parsed = fallback.parse(shoppingList);
            return new Parsed(parsed.lines(), "rule-based (Claude unavailable)");
        }
    }

    private List<ParsedLine> readLines(JsonNode response) throws Exception {
        String text = response.path("content").path(0).path("text").asText("");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON object in the model's reply");
        }
        JsonNode parsed = objectMapper.readTree(text.substring(start, end + 1));

        List<ParsedLine> lines = new ArrayList<>();
        for (JsonNode node : parsed.path("lines")) {
            String sku = node.path("sku").isNull() ? null : node.path("sku").asText(null);
            BigDecimal qty = new BigDecimal(node.path("qty").asText("1"));
            lines.add(new ParsedLine(
                    node.path("sourceText").asText(""),
                    sku == null || sku.isBlank() ? null : sku,
                    qty.signum() > 0 ? qty : BigDecimal.ONE,
                    confidenceOf(node.path("confidence").asText("LOW")),
                    node.path("note").asText("")));
        }
        return lines;
    }

    private Confidence confidenceOf(String value) {
        try {
            return Confidence.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Confidence.LOW;
        }
    }
}
