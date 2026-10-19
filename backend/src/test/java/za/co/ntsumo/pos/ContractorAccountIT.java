package za.co.ntsumo.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The contractor-account paths, against the seeded demo accounts. Each account
 * in the seed exists to put one rule under load: Sithole is over its limit,
 * Khumalo is 90 days overdue and on hold, Mokoena is in good standing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Contractor accounts, over a real database")
class ContractorAccountIT {

    @Autowired
    private MockMvc mvc;

    /** Demo PINs live in the seed; the README lists them. */
    private static final String MOKOENA_OWNER_PIN = "1111";
    private static final String MOKOENA_DRIVER_PIN = "2202";
    private static final String SITHOLE_OWNER_PIN = "1113";

    private String accountSale(UUID uuid, String account, long buyerId, String pin, String job,
                               String sku, String qty, String override) {
        return """
                {"clientUuid":"%s","tillId":"T1","accountCode":"%s","buyerId":%d,"buyerPin":"%s",
                 "jobCode":"%s","orderRef":"PO1234","lines":[{"sku":"%s","qty":%s}]%s}
                """.formatted(uuid, account, buyerId, pin, job, sku, qty,
                override == null ? "" : ",\"overrideReason\":\"%s\",\"overrideBy\":\"johan\"".formatted(override));
    }

    private long buyerId(String accountCode, String name) throws Exception {
        String body = mvc.perform(get("/api/accounts/{code}/buyers", accountCode))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode buyers =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        for (com.fasterxml.jackson.databind.JsonNode buyer : buyers) {
            if (buyer.get("name").asText().equals(name)) {
                return buyer.get("id").asLong();
            }
        }
        throw new IllegalStateException("No buyer " + name + " on " + accountCode);
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("charges a sale to an account in good standing, at trade prices")
    void chargesToHealthyAccount() throws Exception {
        long thabo = buyerId("MOK001", "Thabo Mokoena");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "MOK001", thabo, MOKOENA_OWNER_PIN,
                                "MOK-J01", "BLD-0001", "10", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleType").value("ACCOUNT"))
                .andExpect(jsonPath("$.documentNo").value(startsWith("T1-A")))
                // Tier B is 15% off R108.99.
                .andExpect(jsonPath("$.lines[0].unitPrice").value(92.64))
                .andExpect(jsonPath("$.total").value(926.40))
                .andExpect(jsonPath("$.dueDate").exists())
                // The printed invoice names the account, the collector and the site.
                .andExpect(jsonPath("$.accountName").value("Mokoena Builders"))
                .andExpect(jsonPath("$.buyerName").value("Thabo Mokoena"))
                .andExpect(jsonPath("$.jobDescription").value(containsString("Nkosi house")))
                .andExpect(jsonPath("$.overrideReason").doesNotExist());
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("blocks a sale that would take the account over its limit")
    void blocksOverLimitSale() throws Exception {
        long lerato = buyerId("SIT001", "Lerato Sithole");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "SIT001", lerato, SITHOLE_OWNER_PIN,
                                "SIT-J01", "BLD-0001", "5", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Credit check failed"))
                .andExpect(jsonPath("$.reasons", hasItem(containsString("over the R30000.00 limit"))));
    }

    @Test
    @WithMockUser(username = "johan", roles = "MANAGER")
    @DisplayName("lets a manager override the block, and writes the reason onto the invoice")
    void managerOverrideIsRecorded() throws Exception {
        long lerato = buyerId("SIT001", "Lerato Sithole");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "SIT001", lerato, SITHOLE_OWNER_PIN,
                                "SIT-J01", "BLD-0001", "5", "Payment promised Friday")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overrideReason").value("Payment promised Friday"))
                .andExpect(jsonPath("$.overrideBy").value("johan"));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("refuses the wrong PIN before it ever looks at the credit limit")
    void wrongPinStopsTheSale() throws Exception {
        long thabo = buyerId("MOK001", "Thabo Mokoena");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "MOK001", thabo, "0000",
                                "MOK-J01", "BLD-0001", "1", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Buyer PIN does not match"));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("holds the driver to his own per-sale limit on an otherwise healthy account")
    void driverPerSaleLimit() throws Exception {
        long kagiso = buyerId("MOK001", "Kagiso Molefe");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "MOK001", kagiso, MOKOENA_DRIVER_PIN,
                                "MOK-J01", "BLD-0001", "100", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons", hasItem(containsString("Kagiso Molefe may only charge up to"))));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("blocks an account that is on hold with debt older than 90 days")
    void blocksAccountOnHold() throws Exception {
        long bongani = buyerId("KHU001", "Bongani Khumalo");

        mvc.perform(post("/api/sales/account").contentType(MediaType.APPLICATION_JSON)
                        .content(accountSale(UUID.randomUUID(), "KHU001", bongani, "1114",
                                "KHU-J01", "BLD-0001", "1", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reasons", hasItem(containsString("on hold"))))
                .andExpect(jsonPath("$.reasons", hasItem(containsString("more than 90 days old"))));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    @DisplayName("lists accounts with balances, ageing and a one-word standing")
    void listsAccounts() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.code=='KHU001')].standing").value(hasItem("On hold")))
                .andExpect(jsonPath("$[?(@.code=='SIT001')].standing").value(hasItem("Over limit")))
                .andExpect(jsonPath("$[?(@.code=='MOK001')].standing").value(hasItem("Good standing")));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    @DisplayName("produces a statement whose closing balance matches the ageing total")
    void statementReconciles() throws Exception {
        mvc.perform(get("/api/accounts/{code}/statement", "KHU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.code").value("KHU001"))
                .andExpect(jsonPath("$.lines").isNotEmpty())
                .andExpect(jsonPath("$.ageing.days90Plus").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.ageing.total").value(15726.44))
                .andExpect(jsonPath("$.account.balance").value(15726.44));
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    @DisplayName("takes a payment and settles the oldest invoice first")
    void paymentSettlesOldestFirst() throws Exception {
        mvc.perform(post("/api/accounts/{code}/payments", "MAH001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountCode":"MAH001","amount":1000.00,"method":"EFT","reference":"MAH001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.paymentNo").value(startsWith("RCPT-")))
                .andExpect(jsonPath("$.allocated[0].amount").value(1000.00))
                .andExpect(jsonPath("$.unallocated").value(0.00));
    }

    @Test
    @WithMockUser(username = "thandi", roles = "CASHIER")
    @DisplayName("keeps a cashier out of the payment and dashboard screens")
    void cashierCannotTakePayments() throws Exception {
        mvc.perform(post("/api/accounts/{code}/payments", "MAH001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountCode":"MAH001","amount":100.00,"method":"CASH"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/owner/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner", roles = "OWNER")
    @DisplayName("gives the owner takings, debtors and what to reorder")
    void ownerDashboard() throws Exception {
        mvc.perform(get("/api/owner/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owedByContractors").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.accountsNeedingAttention").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.dailyTakings").isNotEmpty())
                .andExpect(jsonPath("$.reorderSoon").isArray());
    }
}
