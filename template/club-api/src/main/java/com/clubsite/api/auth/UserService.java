package com.clubsite.api.auth;

import com.clubsite.api.auth.AuthDtos.CreateUserRequest;
import com.clubsite.api.auth.AuthDtos.UserSummary;
import com.clubsite.api.common.ApiException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserSummary> listUsers() {
        return userRepository.findAll().stream().map(UserSummary::from).toList();
    }

    public UserSummary createUser(CreateUserRequest request) {
        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw ApiException.conflict("Username already taken: " + username);
        }
        return UserSummary.from(userRepository.save(newUser(username, request.password(), request.role())));
    }

    /** Used by AdminUserSeeder too, so both paths hash the same way. */
    User newUser(String username, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        return user;
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(String id, String requestingUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User", id));

        if (user.getUsername().equals(requestingUsername)) {
            throw ApiException.badRequest("You can't delete your own account.");
        }
        if (user.getRole() == Role.SUPER_ADMIN && userRepository.countByRole(Role.SUPER_ADMIN) <= 1) {
            throw ApiException.badRequest("Can't delete the last super-admin account.");
        }

        userRepository.deleteById(id);
    }
}
