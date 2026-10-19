package za.co.ntsumo.pos.sales;

import org.springframework.stereotype.Service;
import za.co.ntsumo.pos.accounts.ContractorAccount;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.money.Money;

import java.math.BigDecimal;

/**
 * What a given customer pays for a given item.
 *
 * <p>Walk-in customers pay the shelf price. Contractors pay the shelf price less
 * their tier's discount, worked out per unit and then multiplied, so the price
 * printed next to the line is the price the contractor can check by hand.
 */
@Service
public class PricingService {

    public BigDecimal unitPriceFor(Product product, ContractorAccount account) {
        if (account == null) {
            return Money.scaled(product.getRetailInclVat());
        }
        return Money.applyDiscount(product.getRetailInclVat(),
                account.getPriceTier().getDiscountPercent());
    }
}
