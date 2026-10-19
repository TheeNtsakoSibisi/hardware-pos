package za.co.ntsumo.pos.sales;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.testsupport.TestData;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pricing")
class PricingServiceTest {

    private final PricingService pricing = new PricingService();
    private final Product cement = TestData.product("BLD-0001", "Cement 42.5N 50kg", "BAG", "108.99", false);

    @Test
    @DisplayName("a walk-in customer pays the shelf price")
    void walkInPaysShelfPrice() {
        assertThat(pricing.unitPriceFor(cement, null)).isEqualByComparingTo("108.99");
    }

    @Test
    @DisplayName("a tier A contractor gets 10 percent off")
    void tierA() {
        ContractorAccount account = TestData.account("NDL001", "Ndlovu Plumbing", "20000.00", "A");

        assertThat(pricing.unitPriceFor(cement, account)).isEqualByComparingTo("98.09");
    }

    @Test
    @DisplayName("a tier B contractor gets 15 percent off")
    void tierB() {
        ContractorAccount account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");

        assertThat(pricing.unitPriceFor(cement, account)).isEqualByComparingTo("92.64");
    }

    @Test
    @DisplayName("the trade price is still a whole number of cents")
    void tradePriceIsScaled() {
        ContractorAccount account = TestData.account("MOK001", "Mokoena Builders", "60000.00", "B");

        assertThat(pricing.unitPriceFor(cement, account).scale()).isEqualTo(2);
    }
}
