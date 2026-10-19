package za.co.ntsumo.pos.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobSiteRepository extends JpaRepository<JobSite, Long> {

    List<JobSite> findByAccountIdAndActiveTrueOrderByCodeAsc(Long accountId);

    Optional<JobSite> findByCode(String code);
}
