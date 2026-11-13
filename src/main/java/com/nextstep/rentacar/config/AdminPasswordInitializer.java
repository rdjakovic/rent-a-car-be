package com.nextstep.rentacar.config;

import com.nextstep.rentacar.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Sets the seeded 'admin' user's password from ADMIN_PASSWORD at startup.
 * The hash seeded by Flyway V2 matches no known password, so environments without the
 * dev-data migration (e.g. prod) need this to get a working admin. Does nothing when
 * ADMIN_PASSWORD is not set; remove it after the first start if the admin should be able
 * to change the password later.
 */
@Slf4j
@Component
public class AdminPasswordInitializer implements ApplicationRunner {

    static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public AdminPasswordInitializer(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminPassword)) {
            return;
        }
        userRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(admin -> {
            if (passwordEncoder.matches(adminPassword, admin.getPassword())) {
                return;
            }
            admin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(admin);
            log.info("Password of user '{}' set from ADMIN_PASSWORD", ADMIN_USERNAME);
        }, () -> log.warn("ADMIN_PASSWORD is set but no '{}' user exists", ADMIN_USERNAME));
    }
}
