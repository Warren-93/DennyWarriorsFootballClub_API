package mark.warren93.dev.DennyWarriorsAPI.dto;

import java.time.Instant;

/**
 * Login payload returned to the React client.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String username,
        String role,
        Instant expiresAt) {
}
