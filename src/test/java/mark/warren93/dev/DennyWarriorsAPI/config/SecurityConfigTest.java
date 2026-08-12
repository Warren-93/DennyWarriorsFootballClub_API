package mark.warren93.dev.DennyWarriorsAPI.config;

import mark.warren93.dev.DennyWarriorsAPI.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Verifies the security filter chain itself rejects unauthorised requests —
 * these all short-circuit before reaching any controller/repository, so no
 * real MongoDB access happens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void adminEndpointWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/squad"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    void adminUsersEndpointRejectsNonSuperAdminRole() throws Exception {
        String editorToken = jwtService.generateAccessToken("editor-user", "EDITOR");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + editorToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isEqualTo(403);
                });
    }

    @Test
    void adminSquadWriteRejectsViewerRole() throws Exception {
        String viewerToken = jwtService.generateAccessToken("viewer-user", "VIEWER");

        mockMvc.perform(post("/api/v1/admin/squad").header("Authorization", "Bearer " + viewerToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isEqualTo(403);
                });
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken("someone", "SUPER_ADMIN");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + refreshToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }
}
