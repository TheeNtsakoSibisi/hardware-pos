package za.co.ntsumo.pos.testsupport;

import org.springframework.test.util.ReflectionTestUtils;
import za.co.ntsumo.pos.accounts.AuthorisedBuyer;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.accounts.PriceTier;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.payments.PaymentAllocationRepository;
import za.co.ntsumo.pos.sales.Sale;
import za.co.ntsumo.pos.sales.SaleLine;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds domain objects for unit tests.
 *
 * <p>The entities have no public constructors or setters on purpose: outside a
 * test, only the factory methods and JPA may create them. Tests reach past that
 * here, in one place, rather than loosening the entities themselves.
 */
public final class TestData {

    private static final AtomicLong IDS = new AtomicLong(1000);

    private TestData() {
    }

    public static PriceTier tier(String code, String discountPercent) {
        PriceTier tier = instantiate(PriceTier.class);
        ReflectionTestUtils.setField(tier, "code", code);
        ReflectionTestUtils.setField(tier, "name", "Trade " + code);
        ReflectionTestUtils.setField(tier, "discountPercent", new BigDecimal(discountPercent));
        return tier;
    }

    public static ContractorAccount account(String code, String name, String creditLimit, String tierCode) {
        return account(code, name, creditLimit, tierCode, ContractorAccount.Status.ACTIVE);
    }

    public static ContractorAccount account(String code, String name, String creditLimit,
                                            String tierCode, ContractorAccount.Status status) {
        ContractorAccount account = instantiate(ContractorAccount.class);
        ReflectionTestUtils.setField(account, "id", IDS.incrementAndGet());
        ReflectionTestUtils.setField(account, "code", code);
        ReflectionTestUtils.setField(account, "name", name);
        ReflectionTestUtils.setField(account, "creditLimit", new BigDecimal(creditLimit));
        ReflectionTestUtils.setField(account, "priceTier",
                tier(tierCode, tierCode.equals("A") ? "10.00" : "15.00"));
        ReflectionTestUtils.setField(account, "termsDays", 30);
        ReflectionTestUtils.setField(account, "status", status);
        return account;
    }

    public static AuthorisedBuyer buyer(String name, Long accountId, String perSaleLimit, String pinHash) {
        AuthorisedBuyer buyer = instantiate(AuthorisedBuyer.class);
        ReflectionTestUtils.setField(buyer, "id", IDS.incrementAndGet());
        ReflectionTestUtils.setField(buyer, "accountId", accountId);
        ReflectionTestUtils.setField(buyer, "name", name);
        ReflectionTestUtils.setField(buyer, "roleTitle", "Site foreman");
        ReflectionTestUtils.setField(buyer, "pinHash", pinHash);
        ReflectionTestUtils.setField(buyer, "active", true);
        if (perSaleLimit != null) {
            ReflectionTestUtils.setField(buyer, "perSaleLimit", new BigDecimal(perSaleLimit));
        }
        return buyer;
    }

    public static Product product(String sku, String name, String unit, String price, boolean decimalQty) {
        Product product = instantiate(Product.class);
        ReflectionTestUtils.setField(product, "id", IDS.incrementAndGet());
        ReflectionTestUtils.setField(product, "sku", sku);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "category", "Building");
        ReflectionTestUtils.setField(product, "unit", unit);
        ReflectionTestUtils.setField(product, "decimalQty", decimalQty);
        ReflectionTestUtils.setField(product, "retailInclVat", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "costExclVat", new BigDecimal(price));
        ReflectionTestUtils.setField(product, "reorderLevel", BigDecimal.TEN);
        ReflectionTestUtils.setField(product, "active", true);
        return product;
    }

    /** An account invoice as it would come back from the repository. */
    public static Sale accountInvoice(long id, String documentNo, Long accountId, Instant occurredAt,
                                      String total) {
        Sale sale = Sale.onAccount(UUID.randomUUID(), documentNo, "T1", "thandi", occurredAt,
                List.of(new SaleLine(1, 1L, "Seeded line", BigDecimal.ONE, "EACH",
                        new BigDecimal(total), new BigDecimal(total))),
                accountId, 1L, 1L, null, LocalDate.now());
        ReflectionTestUtils.setField(sale, "id", id);
        return sale;
    }

    public static PaymentAllocationRepository.AllocatedAmount allocated(Long saleId, String amount) {
        return new PaymentAllocationRepository.AllocatedAmount() {
            @Override
            public Long getSaleId() {
                return saleId;
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }
        };
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build a test " + type.getSimpleName(), ex);
        }
    }
}
