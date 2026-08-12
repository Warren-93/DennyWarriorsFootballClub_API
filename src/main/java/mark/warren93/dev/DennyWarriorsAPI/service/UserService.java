package mark.warren93.dev.DennyWarriorsAPI.service;

import mark.warren93.dev.DennyWarriorsAPI.dto.CreateUserRequest;
import mark.warren93.dev.DennyWarriorsAPI.dto.UserSummary;
import mark.warren93.dev.DennyWarriorsAPI.exception.UserAlreadyExistsException;
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
}
