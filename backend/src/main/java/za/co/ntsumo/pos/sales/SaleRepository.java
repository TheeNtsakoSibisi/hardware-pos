package za.co.ntsumo.pos.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByClientUuid(UUID clientUuid);

    Optional<Sale> findByDocumentNo(String documentNo);

    List<Sale> findByAccountIdOrderByOccurredAtAsc(Long accountId);

    List<Sale> findTop20ByAccountIdOrderByOccurredAtDesc(Long accountId);

    @Query("SELECT s FROM Sale s WHERE s.occurredAt >= :from AND s.occurredAt < :to ORDER BY s.occurredAt DESC")
    List<Sale> findBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Daily takings, split by how customers paid. Drives the owner dashboard. */
    @Query(value = """
            SELECT (s.occurred_at AT TIME ZONE 'Africa/Johannesburg')::date AS sale_date,
                   COUNT(*)                                                 AS transactions,
                   SUM(s.total_incl_vat)                                    AS total,
                   COALESCE(SUM(t.cash), 0)                                 AS cash,
                   COALESCE(SUM(t.card), 0)                                 AS card,
                   COALESCE(SUM(t.eft), 0)                                  AS eft
            FROM sale s
            LEFT JOIN (
                SELECT sale_id,
                       SUM(CASE WHEN tender_type = 'CASH' THEN amount ELSE 0 END) AS cash,
                       SUM(CASE WHEN tender_type = 'CARD' THEN amount ELSE 0 END) AS card,
                       SUM(CASE WHEN tender_type = 'EFT'  THEN amount ELSE 0 END) AS eft
                FROM sale_tender GROUP BY sale_id
            ) t ON t.sale_id = s.id
            WHERE s.sale_type = 'WALK_IN'
              AND (s.occurred_at AT TIME ZONE 'Africa/Johannesburg')::date >= :from
            GROUP BY 1 ORDER BY 1
            """, nativeQuery = true)
    List<DailyTakings> walkInTakingsSince(@Param("from") LocalDate from);

    /** Rand value sold per product on account, for "what do contractors buy?". */
    @Query(value = """
            SELECT p.name AS name, SUM(l.line_total_incl_vat) AS value
            FROM sale_line l
            JOIN sale s ON s.id = l.sale_id
            JOIN product p ON p.id = l.product_id
            WHERE s.sale_type = 'ACCOUNT'
            GROUP BY p.name ORDER BY 2 DESC LIMIT :limit
            """, nativeQuery = true)
    List<ProductValue> topAccountProducts(@Param("limit") int limit);

    interface DailyTakings {
        java.sql.Date getSaleDate();

        long getTransactions();

        BigDecimal getTotal();

        BigDecimal getCash();

        BigDecimal getCard();

        BigDecimal getEft();
    }

    interface ProductValue {
        String getName();

        BigDecimal getValue();
    }
}
