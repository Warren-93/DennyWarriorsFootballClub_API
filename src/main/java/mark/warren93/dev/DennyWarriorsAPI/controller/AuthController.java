package mark.warren93.dev.DennyWarriorsAPI.controller;

import jakarta.validation.Valid;
import mark.warren93.dev.DennyWarriorsAPI.dto.LoginRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.LoginResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.RefreshRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.TokenResponse;
import mark.warren93.dev.DennyWarriorsAPI.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }
}
