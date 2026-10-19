package za.co.ntsumo.pos.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsernameAndActiveTrue(String username);
}
