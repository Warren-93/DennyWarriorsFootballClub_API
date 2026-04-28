package mark.warren93.dev.DennyWarriorsAPI.dto;

import java.time.Instant;

/**
 * Login payload returned to the React client.
 * The `token` field is what client.js#extractToken picks up.
 */
public record LoginResponse(
        String token,
        String username,
        boolean admin,
        Instant expiresAt) {
}
