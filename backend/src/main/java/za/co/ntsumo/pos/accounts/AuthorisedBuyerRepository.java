package za.co.ntsumo.pos.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorisedBuyerRepository extends JpaRepository<AuthorisedBuyer, Long> {

    List<AuthorisedBuyer> findByAccountIdAndActiveTrueOrderByNameAsc(Long accountId);
}
