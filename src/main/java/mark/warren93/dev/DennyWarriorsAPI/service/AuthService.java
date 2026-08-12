package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.dto.LoginRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.LoginResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.TokenResponse;
import mark.warren93.dev.DennyWarriorsAPI.model.User;
import mark.warren93.dev.DennyWarriorsAPI.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
    }

    public LoginResponse login(LoginRequest request) {
        loginRateLimiter.checkAllowed(request.username());

        User user = userRepository.findByUserName(request.username()).orElse(null);
        if (user == null || !passwordMatches(request.password(), user.getPassword())) {
            loginRateLimiter.recordFailure(request.username());
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getRole() == null) {
            loginRateLimiter.recordFailure(request.username());
            throw new BadCredentialsException("User is not authorised for the admin area");
        }

        loginRateLimiter.recordSuccess(request.username());

        String role = user.getRole().name();
        String accessToken = jwtService.generateAccessToken(user.getUserName(), role);
        String refreshToken = jwtService.generateRefreshToken(user.getUserName(), role);
        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getUserName(),
                role,
                jwtService.expiryOf(jwtService.expirationMs()));
    }

    public TokenResponse refresh(String refreshToken) {
        Map<String, Object> claims = jwtService.parseRefreshToken(refreshToken);
        String username = String.valueOf(claims.get("sub"));
        String role = String.valueOf(claims.get("role"));

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (user.getRole() == null || !user.getRole().name().equals(role)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String accessToken = jwtService.generateAccessToken(username, role);
        return new TokenResponse(accessToken, jwtService.expiryOf(jwtService.expirationMs()));
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
