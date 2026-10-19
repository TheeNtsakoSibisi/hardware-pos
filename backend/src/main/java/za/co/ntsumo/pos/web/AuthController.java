package za.co.ntsumo.pos.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import za.co.ntsumo.pos.security.Staff;
import za.co.ntsumo.pos.security.StaffRepository;

/** Sign in at the start of a shift; the session cookie lasts the shift. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final StaffRepository staff;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, StaffRepository staff) {
        this.authenticationManager = authenticationManager;
        this.staff = staff;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record SessionView(String username, String displayName, Staff.Role role) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, httpRequest, httpResponse);
            return ResponseEntity.ok(sessionView(request.username()));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(ApiError.of("Username or password is incorrect"));
        }
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(ApiError.of("Not signed in"));
        }
        return ResponseEntity.ok(sessionView(authentication.getName()));
    }

    private SessionView sessionView(String username) {
        Staff found = staff.findByUsernameAndActiveTrue(username).orElseThrow();
        return new SessionView(found.getUsername(), found.getDisplayName(), found.getRole());
    }
}
