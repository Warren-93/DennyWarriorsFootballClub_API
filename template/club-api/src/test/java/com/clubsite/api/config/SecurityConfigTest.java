package com.clubsite.api.config;

import com.clubsite.api.auth.JwtService;
import com.clubsite.api.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the security rules end to end. Every request here is rejected
 * (or fails validation) before reaching a repository, so no MongoDB is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateAccessToken(role.name().toLowerCase(), role).token();
    }

    @Test
    void adminWriteWithoutTokenIs401SoTheClientKnowsToRefresh() throws Exception {
        mockMvc.perform(post("/api/v1/admin/squad")).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/news").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        String refresh = jwtService.generateRefreshToken("someone", Role.SUPER_ADMIN).token();

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userManagementIsSuperAdminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(Role.EDITOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotWrite() throws Exception {
        mockMvc.perform(post("/api/v1/admin/squad").header("Authorization", bearer(Role.VIEWER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/sync/trigger").header("Authorization", bearer(Role.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changingPasswordNeedsAToken() throws Exception {
        mockMvc.perform(put("/api/v1/account/password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBodyFromAnEditorIs400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/admin/squad")
                        .header("Authorization", bearer(Role.EDITOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"Smith\",\"position\":\"Forward\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void invalidStandingsRowIs400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/standings")
                        .header("Authorization", bearer(Role.EDITOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"position\":0,\"team\":\"\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginBodyIsValidatedBeforeAnyLookup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsPreflightFromTheConfiguredOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/news")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflightFromAnUnknownOriginIsRefused() throws Exception {
        mockMvc.perform(options("/api/v1/news")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
