package za.co.ntsumo.pos.payments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountPaymentRepository extends JpaRepository<AccountPayment, Long> {

    List<AccountPayment> findByAccountIdOrderByPaymentDateAsc(Long accountId);
}
