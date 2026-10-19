package za.co.ntsumo.pos.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/** Looks staff up by username for Spring Security. */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staff;

    public StaffUserDetailsService(StaffRepository staff) {
        this.staff = staff;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Staff found = staff.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("No active staff member " + username));
        return User.withUsername(found.getUsername())
                .password(found.getPasswordHash())
                .roles(found.getRole().name())
                .build();
    }
}
