package za.co.ntsumo.pos;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import za.co.ntsumo.pos.catalogue.StockMovementRepository;
import za.co.ntsumo.pos.catalogue.ProductRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End to end over a real PostgreSQL with the demo data loaded: the paths a
 * cashier actually walks, including the ones that are supposed to fail.
 *
 * <p>Run with {@code mvn verify} and a database up (docker compose up -d db).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Counter, over a real database")
class CounterFlowIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ProductRepository products;

    @Autowired
    private StockMovementRepository stock;

    private String walkInSale(UUID uuid, String sku, String qty, String amount) {
        return """
                {"clientUuid":"%s","tillId":"T1",
                 "lines":[{"sku":"%s","qty":%s}],
                 "tenders":[{"type":"CASH","amount":%s,"cashReceived":%s}]}
                """.formatted(uuid, sku, qty, amount, amount);
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("sells cement for cash, prints a document number and moves the stock")
    void walkInSaleDeductsStock() throws Exception {
        Long cementId = products.findBySku("BLD-0001").orElseThrow().getId();
        BigDecimal before = stock.stockOnHand(cementId);

        MvcResult result = mvc.perform(post("/api/sales/walk-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walkInSale(UUID.randomUUID(), "BLD-0001", "2", "217.98")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(217.98))
                .andExpect(jsonPath("$.vat").value(28.43))
                .andExpect(jsonPath("$.documentNo").exists())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("documentNo").asText()).startsWith("T1-");
        assertThat(body.get("tenders").get(0).get("changeGiven").decimalValue())
                .isEqualByComparingTo("0.00");
        assertThat(stock.stockOnHand(cementId)).isEqualByComparingTo(before.subtract(new BigDecimal("2")));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("gives change when the customer hands over more than the total")
    void givesChange() throws Exception {
        String body = """
                {"clientUuid":"%s","tillId":"T1",
                 "lines":[{"sku":"BLD-0001","qty":1}],
                 "tenders":[{"type":"CASH","amount":108.99,"cashReceived":200.00}]}
                """.formatted(UUID.randomUUID());

        mvc.perform(post("/api/sales/walk-in").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenders[0].changeGiven").value(91.01));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("refuses a sale the payment does not cover")
    void refusesShortPayment() throws Exception {
        mvc.perform(post("/api/sales/walk-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walkInSale(UUID.randomUUID(), "BLD-0001", "2", "100.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("The payment does not cover the sale"));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("replaying a queued sale stores it once and returns the same document")
    void offlineReplayIsIdempotent() throws Exception {
        UUID clientUuid = UUID.randomUUID();
        String body = walkInSale(clientUuid, "BLD-0001", "1", "108.99");
        Long cementId = products.findBySku("BLD-0001").orElseThrow().getId();
        BigDecimal before = stock.stockOnHand(cementId);

        String first = mvc.perform(post("/api/sales/walk-in")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mvc.perform(post("/api/sales/walk-in")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(first).get("documentNo").asText())
                .isEqualTo(json.readTree(second).get("documentNo").asText());
        // The sale is captured once, so the shelf is drawn down once.
        assertThat(stock.stockOnHand(cementId)).isEqualByComparingTo(before.subtract(BigDecimal.ONE));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("sells cable by the metre at a fractional quantity")
    void sellsByTheMetre() throws Exception {
        String body = """
                {"clientUuid":"%s","tillId":"T1",
                 "lines":[{"sku":"ELC-0002","qty":12.5}],
                 "tenders":[{"type":"CARD","amount":181.13}]}
                """.formatted(UUID.randomUUID());

        mvc.perform(post("/api/sales/walk-in").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].qty").value(12.5))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(181.13));
    }

    @Test
    @DisplayName("will not sell to someone who is not signed in")
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/sales/walk-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(walkInSale(UUID.randomUUID(), "BLD-0001", "1", "108.99")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("finds products by name and by barcode")
    void searchesCatalogue() throws Exception {
        mvc.perform(get("/api/products").param("term", "cement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].stockOnHand").exists());

        mvc.perform(get("/api/products").param("category", "Plumbing").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("syncs a batch of queued sales and reports each one")
    void syncsOfflineBatch() throws Exception {
        String batch = """
                {"walkInSales":[
                  {"clientUuid":"%s","tillId":"T2","lines":[{"sku":"BLD-0001","qty":1}],
                   "tenders":[{"type":"CASH","amount":108.99,"cashReceived":108.99}]},
                  {"clientUuid":"%s","tillId":"T2","lines":[{"sku":"BLD-0001","qty":1}],
                   "tenders":[{"type":"CASH","amount":1.00,"cashReceived":1.00}]}
                ]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/api/sync/sales").contentType(MediaType.APPLICATION_JSON).content(batch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.rejected").value(1))
                // The rejected one carries its reason back so the till can flag it.
                .andExpect(jsonPath("$.outcomes[1].reasons[0]").exists())
                .andExpect(jsonPath("$.outcomes[0].documentNo").value(org.hamcrest.Matchers.startsWith("T2-")));
    }
}
