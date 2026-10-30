package com.clubsite.api.auth;

import com.clubsite.api.TestProperties;
import com.clubsite.api.auth.AuthDtos.LoginRequest;
import com.clubsite.api.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Minimum BCrypt cost keeps the test fast; production uses the default (10).
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService = new JwtService(TestProperties.jwt());

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtService, encoder, new LoginRateLimiter(TestProperties.defaults()));
    }

    private User user(String username, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        return user;
    }

    @Test
    void validCredentialsReturnBothTokens() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", "correct horse battery", Role.SUPER_ADMIN)));

        var response = authService.login(new LoginRequest("admin", "correct horse battery"));

        assertThat(response.role()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(jwtService.parseAccessToken(response.accessToken())).containsEntry("sub", "admin");
        assertThat(jwtService.parseRefreshToken(response.refreshToken())).containsEntry("sub", "admin");
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameError() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", "correct horse battery", Role.SUPER_ADMIN)));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Invalid credentials");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "wrong")))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Invalid credentials");
    }

    @Test
    void plaintextStoredPasswordIsNeverAccepted() {
        User legacy = new User();
        legacy.setUsername("admin");
        legacy.setPasswordHash("plaintext-password");
        legacy.setRole(Role.SUPER_ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "plaintext-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void lockedOutAfterRepeatedFailures() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin", "correct horse battery", Role.SUPER_ADMIN)));

        for (int i = 0; i < LoginRateLimiter.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        // Even the right password is refused while locked out.
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "correct horse battery")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Too many");
    }

    @Test
    void refreshIsRefusedOnceTheUsersRoleHasChanged() {
        String refresh = jwtService.generateRefreshToken("editor", Role.EDITOR).token();
        when(userRepository.findByUsername("editor")).thenReturn(Optional.of(user("editor", "irrelevant-password", Role.VIEWER)));

        assertThatThrownBy(() -> authService.refresh(refresh)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshIsRefusedForADeletedUser() {
        String refresh = jwtService.generateRefreshToken("gone", Role.EDITOR).token();
        when(userRepository.findByUsername("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refresh)).isInstanceOf(BadCredentialsException.class);
    }
}
