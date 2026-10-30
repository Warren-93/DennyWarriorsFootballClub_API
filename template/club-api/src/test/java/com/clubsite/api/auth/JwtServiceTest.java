package com.clubsite.api.auth;

import com.clubsite.api.TestProperties;
import com.clubsite.api.config.ClubProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TestProperties.jwt());

    @Test
    void refusesToStartWithoutASecret() {
        assertThatThrownBy(() -> new JwtService(new ClubProperties.Jwt(null, Duration.ofHours(1), Duration.ofDays(1), "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> new JwtService(new ClubProperties.Jwt("too-short", Duration.ofHours(1), Duration.ofDays(1), "x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void issuesAndParsesAccessTokens() {
        String token = jwtService.generateAccessToken("admin", Role.SUPER_ADMIN).token();

        assertThat(jwtService.parseAccessToken(token))
                .containsEntry("sub", "admin")
                .containsEntry("role", "SUPER_ADMIN")
                .containsEntry("type", "access");
    }

    @Test
    void refreshTokenIsNotAnAccessTokenAndViceVersa() {
        String access = jwtService.generateAccessToken("admin", Role.EDITOR).token();
        String refresh = jwtService.generateRefreshToken("admin", Role.EDITOR).token();

        assertThatThrownBy(() -> jwtService.parseAccessToken(refresh)).isInstanceOf(JwtService.InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.parseRefreshToken(access)).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsTamperedPayload() {
        String token = jwtService.generateAccessToken("editor", Role.EDITOR).token();
        String[] parts = token.split("\\.");
        String forgedClaims = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(parts[1])).replace("EDITOR", "SUPER_ADMIN").getBytes());

        assertThatThrownBy(() -> jwtService.parse(parts[0] + "." + forgedClaims + "." + parts[2]))
                .isInstanceOf(JwtService.InvalidTokenException.class)
                .hasMessageContaining("Signature");
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService(new ClubProperties.Jwt("b".repeat(32), Duration.ofHours(1), Duration.ofDays(1), "club-api"));
        String foreign = other.generateAccessToken("admin", Role.SUPER_ADMIN).token();

        assertThatThrownBy(() -> jwtService.parse(foreign)).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService instantExpiry = new JwtService(new ClubProperties.Jwt(TestProperties.SECRET, Duration.ZERO, Duration.ZERO, "club-api"));
        String token = instantExpiry.generateAccessToken("admin", Role.SUPER_ADMIN).token();

        assertThatThrownBy(() -> instantExpiry.parse(token))
                .isInstanceOf(JwtService.InvalidTokenException.class)
                .hasMessageContaining("expired");
    }
}
