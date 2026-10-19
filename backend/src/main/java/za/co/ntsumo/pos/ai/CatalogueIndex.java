package za.co.ntsumo.pos.ai;

import org.springframework.stereotype.Component;
import za.co.ntsumo.pos.catalogue.Product;
import za.co.ntsumo.pos.catalogue.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

/** The catalogue in the compact form both parsers work from. */
@Component
public class CatalogueIndex {

    private final ProductRepository products;

    public CatalogueIndex(ProductRepository products) {
        this.products = products;
    }

    public List<Product> all() {
        return products.findAll().stream().filter(Product::isActive).toList();
    }

    /** One line per product: SKU, name, unit. Short enough to send whole. */
    public String asText() {
        return all().stream()
                .map(p -> "%s|%s|per %s".formatted(p.getSku(), p.getName(), p.getUnit()))
                .collect(Collectors.joining("\n"));
    }
}
