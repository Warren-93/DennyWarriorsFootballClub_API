package mark.warren93.dev.DennyWarriorsAPI.dto;

import java.time.Instant;

public record TokenResponse(String accessToken, Instant expiresAt) {
}
