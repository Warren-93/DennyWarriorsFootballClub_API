package mark.warren93.dev.DennyWarriorsAPI.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "denny-warriors-api";

    @Test
    void refusesToStartWithoutASecret() {
        assertThatThrownBy(() -> new JwtService(null, 60_000, 120_000, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DWFC_JWT_SECRET");
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 60_000, 120_000, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DWFC_JWT_SECRET");
    }

    @Test
    void issuesAndParsesTokensWithALongEnoughSecret() {
        JwtService jwtService = new JwtService("a".repeat(32), 60_000, 120_000, ISSUER);

        String token = jwtService.generateAccessToken("admin", "SUPER_ADMIN");

        assertThat(jwtService.parse(token)).containsEntry("sub", "admin").containsEntry("role", "SUPER_ADMIN");
    }
}
