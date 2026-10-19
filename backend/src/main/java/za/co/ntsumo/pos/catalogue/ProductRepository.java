package za.co.ntsumo.pos.catalogue;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:category IS NULL OR p.category = :category)
              AND (:term IS NULL OR lower(p.name) LIKE lower(concat('%', :term, '%'))
                   OR lower(p.sku) LIKE lower(concat(:term, '%'))
                   OR p.barcode = :term)
            ORDER BY p.name
            """)
    List<Product> search(@Param("term") String term, @Param("category") String category, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.active = true ORDER BY p.category")
    List<String> findCategories();
}
