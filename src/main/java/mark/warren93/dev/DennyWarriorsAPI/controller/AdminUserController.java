package mark.warren93.dev.DennyWarriorsAPI.controller;

import jakarta.validation.Valid;
import mark.warren93.dev.DennyWarriorsAPI.dto.ApiListResponse;
import mark.warren93.dev.DennyWarriorsAPI.dto.CreateUserRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.UserSummary;
import mark.warren93.dev.DennyWarriorsAPI.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiListResponse<UserSummary> getUsers() {
        return ApiListResponse.of(userService.listUsers());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}
