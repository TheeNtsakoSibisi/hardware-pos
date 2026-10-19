package za.co.ntsumo.pos.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractorAccountRepository extends JpaRepository<ContractorAccount, Long> {

    Optional<ContractorAccount> findByCode(String code);

    List<ContractorAccount> findAllByOrderByNameAsc();
}
