package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.dto.CreateUserRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.UserSummary;
import mark.warren93.dev.DennyWarriorsAPI.exception.InvalidUserOperationException;
import mark.warren93.dev.DennyWarriorsAPI.exception.ResourceNotFoundException;
import mark.warren93.dev.DennyWarriorsAPI.exception.UserAlreadyExistsException;
import mark.warren93.dev.DennyWarriorsAPI.model.Role;
import mark.warren93.dev.DennyWarriorsAPI.model.User;
import mark.warren93.dev.DennyWarriorsAPI.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        userRepository.findByUserName(request.username()).ifPresent(existing -> {
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        });

        User user = new User();
        user.setUserName(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        return UserSummary.from(userRepository.save(user));
    }

    public void deleteUser(String id, String requestingUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (user.getUserName().equals(requestingUsername)) {
            throw new InvalidUserOperationException("You can't delete your own account.");
        }

        if (user.getRole() == Role.SUPER_ADMIN && userRepository.countByRole(Role.SUPER_ADMIN) <= 1) {
            throw new InvalidUserOperationException("Can't delete the last super-admin account.");
        }

        userRepository.deleteById(id);
    }
}
