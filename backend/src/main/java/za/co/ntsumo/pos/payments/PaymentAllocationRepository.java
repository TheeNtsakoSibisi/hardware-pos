package za.co.ntsumo.pos.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query("""
            SELECT a.saleId AS saleId, COALESCE(SUM(a.amount), 0) AS amount
            FROM PaymentAllocation a
            WHERE a.saleId IN :saleIds
            GROUP BY a.saleId
            """)
    List<AllocatedAmount> sumBySaleIds(@Param("saleIds") List<Long> saleIds);

    List<PaymentAllocation> findByPaymentId(Long paymentId);

    interface AllocatedAmount {
        Long getSaleId();

        BigDecimal getAmount();
    }
}
