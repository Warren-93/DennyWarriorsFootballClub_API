package com.clubsite.api.auth;

import com.clubsite.api.common.Text;
import com.clubsite.api.config.ClubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Creates the first SUPER_ADMIN on startup when the users collection is
 * empty, so a fresh deployment can be logged into. After that it does
 * nothing; set ADMIN_BOOTSTRAP_ENABLED=false once the first login works.
 */
@Component
@ConditionalOnProperty(value = "club.admin.bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final ClubProperties.Admin admin;

    public AdminUserSeeder(UserRepository userRepository, UserService userService, ClubProperties properties) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.admin = properties.admin();
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        String password = admin.bootstrapPassword();
        if (Text.isBlank(password) || password.length() < AuthDtos.MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("No users exist yet: set ADMIN_PASSWORD (at least "
                    + AuthDtos.MIN_PASSWORD_LENGTH + " characters) so the first admin can be created");
        }

        userRepository.save(userService.newUser(admin.bootstrapUsername(), password, Role.SUPER_ADMIN));
        log.info("Created the first admin user '{}'.", admin.bootstrapUsername());
    }
}
