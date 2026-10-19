package za.co.ntsumo.pos.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.catalogue.ProductRepository;
import za.co.ntsumo.pos.testsupport.TestData;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A builder's list is not a product list. These are the shapes that actually
 * arrive by WhatsApp, written the way a builder writes them.
 */
@DisplayName("Rule-based quote parser")
class RuleBasedShoppingListParserTest {

    private RuleBasedShoppingListParser parser;

    @BeforeEach
    void setUp() {
        List<Product> catalogue = List.of(
                TestData.product("BLD-0001", "Cement 42.5N 50kg", "BAG", "108.99", false),
                TestData.product("BLD-0002", "Cement 32.5R 50kg", "BAG", "98.99", false),
                TestData.product("BLD-0007", "Building sand (loose)", "M3", "649.99", true),
                TestData.product("BLD-0009", "Stock brick NFP", "EACH", "2.99", false),
                TestData.product("BLD-0014", "Lintel 2.4m", "EACH", "168.99", false),
                TestData.product("PLB-0004", "PVC sewer pipe 110mm 6m", "LENGTH", "399.99", false),
                TestData.product("PLB-0005", "PVC bend 110mm 90deg", "EACH", "47.99", false),
                TestData.product("ELC-0002", "Surfix cable 2.5mm 3-core", "M", "14.49", true),
                TestData.product("FST-0004", "Chipboard screw 4x40 (box 200)", "BOX", "68.99", false),
                TestData.product("PNT-0003", "PVA white 20L", "TIN", "799.99", false));

        ProductRepository products = mock(ProductRepository.class);
        when(products.findAll()).thenReturn(catalogue);
        parser = new RuleBasedShoppingListParser(new CatalogueIndex(products));
    }

    private Map<String, ShoppingListParser.ParsedLine> parse(String list) {
        return parser.parse(list).lines().stream()
                .collect(Collectors.toMap(ShoppingListParser.ParsedLine::sourceText, Function.identity()));
    }

    @Test
    @DisplayName("reads a whole builder's list from WhatsApp")
    void parsesRealList() {
        String list = """
                Mandla - Protea Glen job
                10 pockets cement
                2 cube sand
                500 stock bricks
                6 x 110 pipe
                4 bends 110
                3 boxes 4x40 chipboard screws
                20m surfix 2.5
                1 pva white 20L
                2 lintels 2.4
                """;

        Map<String, ShoppingListParser.ParsedLine> lines = parse(list);

        // The job heading is not an item and is left out rather than flagged.
        assertThat(lines).doesNotContainKey("Mandla - Protea Glen job");
        assertThat(lines).hasSize(9);
        assertThat(lines.values()).allSatisfy(line -> assertThat(line.sku()).isNotNull());

        assertThat(lines.get("10 pockets cement").sku()).isEqualTo("BLD-0001");
        assertThat(lines.get("10 pockets cement").qty()).isEqualByComparingTo("10");
        assertThat(lines.get("2 cube sand").sku()).isEqualTo("BLD-0007");
        assertThat(lines.get("500 stock bricks").sku()).isEqualTo("BLD-0009");
        assertThat(lines.get("6 x 110 pipe").sku()).isEqualTo("PLB-0004");
        assertThat(lines.get("6 x 110 pipe").qty()).isEqualByComparingTo("6");
        assertThat(lines.get("4 bends 110").sku()).isEqualTo("PLB-0005");
        assertThat(lines.get("3 boxes 4x40 chipboard screws").sku()).isEqualTo("FST-0004");
        assertThat(lines.get("20m surfix 2.5").sku()).isEqualTo("ELC-0002");
        assertThat(lines.get("20m surfix 2.5").qty()).isEqualByComparingTo("20");
        assertThat(lines.get("1 pva white 20L").sku()).isEqualTo("PNT-0003");
        assertThat(lines.get("2 lintels 2.4").sku()).isEqualTo("BLD-0014");
    }

    @Test
    @DisplayName("knows a box of screws from loose screws")
    void plurals() {
        assertThat(parse("3 boxes 4x40 chipboard screws").get("3 boxes 4x40 chipboard screws").sku())
                .isEqualTo("FST-0004");
    }

    @Test
    @DisplayName("tells the two cements apart by their strength")
    void distinguishesBySize() {
        assertThat(parse("5 cement 32.5").get("5 cement 32.5").sku()).isEqualTo("BLD-0002");
        assertThat(parse("5 cement 42.5").get("5 cement 42.5").sku()).isEqualTo("BLD-0001");
    }

    @Test
    @DisplayName("assumes one when no quantity is given, and says so")
    void defaultsQuantityToOne() {
        ShoppingListParser.ParsedLine line = parse("pva white 20L").get("pva white 20L");

        assertThat(line.qty()).isEqualByComparingTo("1");
        assertThat(line.confidence()).isNotEqualTo(ShoppingListParser.Confidence.HIGH);
        assertThat(line.note()).isNotBlank();
    }

    @Test
    @DisplayName("flags a line it cannot place rather than guessing a product")
    void flagsUnknownItem() {
        ShoppingListParser.ParsedLine line = parse("4 solar inverters").get("4 solar inverters");

        assertThat(line.sku()).isNull();
        assertThat(line.confidence()).isEqualTo(ShoppingListParser.Confidence.LOW);
        assertThat(line.note()).contains("choose the product");
    }

    @Test
    @DisplayName("takes a decimal quantity for anything sold by the metre")
    void decimalQuantity() {
        assertThat(parse("12.5 surfix 2.5").get("12.5 surfix 2.5").qty()).isEqualByComparingTo("12.5");
    }

    @Test
    @DisplayName("ignores greetings and blank lines")
    void ignoresChatter() {
        assertThat(parser.parse("Hi there\n\n  \nthanks\n10 pockets cement").lines()).hasSize(1);
    }
}
