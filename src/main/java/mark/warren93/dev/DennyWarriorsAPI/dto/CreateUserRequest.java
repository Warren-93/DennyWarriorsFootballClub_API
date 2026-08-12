package mark.warren93.dev.DennyWarriorsAPI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import mark.warren93.dev.DennyWarriorsAPI.model.Role;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull Role role) {
}
