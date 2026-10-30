package com.clubsite.api.auth;

import com.clubsite.api.auth.AuthDtos.LoginRequest;
import com.clubsite.api.auth.AuthDtos.LoginResponse;
import com.clubsite.api.auth.AuthDtos.TokenResponse;
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
    /*
     * Compared against when the username doesn't exist, so an unknown user
     * costs the same BCrypt work as a wrong password. Without it, response
     * time reveals which usernames are real.
     */
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.loginRateLimiter = loginRateLimiter;
        this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
    }

    public LoginResponse login(LoginRequest request) {
        loginRateLimiter.checkAllowed(request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);
        boolean matches = passwordEncoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches || user.getRole() == null) {
            loginRateLimiter.recordFailure(request.username());
            throw new BadCredentialsException("Invalid credentials");
        }

        loginRateLimiter.recordSuccess(request.username());

        var access = jwtService.generateAccessToken(user.getUsername(), user.getRole());
        var refresh = jwtService.generateRefreshToken(user.getUsername(), user.getRole());
        return new LoginResponse(access.token(), refresh.token(), user.getUsername(), user.getRole(), access.expiresAt());
    }

    /**
     * Re-reads the user, so a deleted account or a changed role stops working
     * at the next refresh rather than living on for the refresh token's lifetime.
     */
    public TokenResponse refresh(String refreshToken) {
        Map<String, Object> claims = jwtService.parseRefreshToken(refreshToken);
        String username = String.valueOf(claims.get("sub"));
        String role = String.valueOf(claims.get("role"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (user.getRole() == null || !user.getRole().name().equals(role)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        var access = jwtService.generateAccessToken(username, user.getRole());
        return new TokenResponse(access.token(), access.expiresAt());
    }
}
