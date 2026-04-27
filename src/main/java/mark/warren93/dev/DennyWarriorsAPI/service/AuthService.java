package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.dto.LoginRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.LoginResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.User;
import mark.warren93.dev.DennyWarriorsAPI.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserName(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordMatches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isAdmin()) {
            // Site is read-only for non-admins; only admins need a token today.
            throw new BadCredentialsException("User is not authorised for the admin area");
        }

        String token = jwtService.generateToken(user.getUserName(), user.isAdmin());
        return new LoginResponse(
                token,
                user.getUserName(),
                user.isAdmin(),
                jwtService.expiryOf(jwtService.expirationMs()));
    }

    /**
     * Compare a presented password against the stored value. Supports both
     * BCrypt-hashed entries (preferred) and legacy plaintext entries seeded
     * during development.
     */
    private boolean passwordMatches(String presented, String stored) {
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(presented, stored);
        }
        return stored.equals(presented);
    }
}
