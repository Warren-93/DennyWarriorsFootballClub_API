package mark.warren93.dev.DennyWarriorsAPI.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}
