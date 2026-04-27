package mark.warren93.dev.DennyWarriorsAPI.config;

import mark.warren93.dev.DennyWarriorsAPI.model.User;
import mark.warren93.dev.DennyWarriorsAPI.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * On startup, if no users exist in the database, create a default admin
 * using dwfc.admin.bootstrap.* properties. Lets you log in immediately
 * for local dev without manually inserting a Mongo document.
 */
@Component
@ConditionalOnProperty(value = "dwfc.admin.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${dwfc.admin.bootstrap.username}") String username,
            @Value("${dwfc.admin.bootstrap.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = new User();
        admin.setUserName(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setAdmin(true);
        admin.setManager(false);
        admin.setPlayer(false);
        userRepository.save(admin);

        log.info("Seeded initial admin user '{}' (change the password before going live!).", username);
    }
}
