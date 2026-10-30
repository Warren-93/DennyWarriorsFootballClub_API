package com.clubsite.api.auth;

import com.clubsite.api.auth.AuthDtos.CreateUserRequest;
import com.clubsite.api.auth.AuthDtos.UserSummary;
import com.clubsite.api.common.ApiListResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** SUPER_ADMIN only (see SecurityConfig). */
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id, Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
    }
}
