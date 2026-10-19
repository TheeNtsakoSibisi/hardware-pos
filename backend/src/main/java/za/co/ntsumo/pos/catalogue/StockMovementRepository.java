package za.co.ntsumo.pos.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT COALESCE(SUM(m.qtyDelta), 0) FROM StockMovement m WHERE m.productId = :productId")
    BigDecimal stockOnHand(@Param("productId") Long productId);

    @Query("""
            SELECT m.productId AS productId, COALESCE(SUM(m.qtyDelta), 0) AS onHand
            FROM StockMovement m GROUP BY m.productId
            """)
    List<StockLevel> allStockLevels();

    /** Products at or below their reorder level, lowest cover first. */
    @Query(value = """
            SELECT p.id AS product_id, p.sku, p.name, p.unit,
                   COALESCE(SUM(m.qty_delta), 0) AS on_hand, p.reorder_level
            FROM product p LEFT JOIN stock_movement m ON m.product_id = p.id
            WHERE p.active = true
            GROUP BY p.id, p.sku, p.name, p.unit, p.reorder_level
            HAVING COALESCE(SUM(m.qty_delta), 0) <= p.reorder_level
            ORDER BY CASE WHEN p.reorder_level = 0 THEN 0
                          ELSE COALESCE(SUM(m.qty_delta), 0) / p.reorder_level END
            """, nativeQuery = true)
    List<LowStockRow> findLowStock();

    interface StockLevel {
        Long getProductId();

        BigDecimal getOnHand();
    }

    interface LowStockRow {
        Long getProductId();

        String getSku();

        String getName();

        String getUnit();

        BigDecimal getOnHand();

        BigDecimal getReorderLevel();
    }
}
