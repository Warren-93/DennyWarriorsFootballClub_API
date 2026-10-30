package com.clubsite.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Request/response bodies for the auth, account and user-management endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static final int MIN_PASSWORD_LENGTH = 12;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String username,
            Role role,
            Instant expiresAt) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, Instant expiresAt) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH) String newPassword) {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH) String password,
            @NotNull Role role) {
    }

    public record UserSummary(String id, String username, Role role, Instant createdAt) {
        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
        }
    }
}
