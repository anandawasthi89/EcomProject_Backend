package com.project.ecomapp.ecommerce_Project.user.bootstrap;

import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevUserBootstrap implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevUserBootstrap.class);

    private final UserRepDAO userRepDAO;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;
    private final String managerEmail;
    private final String managerPassword;
    private final String managerName;

    public DevUserBootstrap(
            UserRepDAO userRepDAO,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.demo-users.enabled:true}") boolean enabled,
            @Value("${app.bootstrap.admin.email:admin@example.com}") String adminEmail,
            @Value("${app.bootstrap.admin.password:admin12345}") String adminPassword,
            @Value("${app.bootstrap.admin.name:Dev Admin}") String adminName,
            @Value("${app.bootstrap.manager.email:manager@example.com}") String managerEmail,
            @Value("${app.bootstrap.manager.password:manager12345}") String managerPassword,
            @Value("${app.bootstrap.manager.name:Dev Manager}") String managerName
    ) {
        this.userRepDAO = userRepDAO;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
        this.managerEmail = managerEmail;
        this.managerPassword = managerPassword;
        this.managerName = managerName;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            LOGGER.info("Dev demo-user bootstrap is disabled");
            return;
        }

        upsertBootstrapUser(adminEmail, adminPassword, adminName, UserRole.ADMIN);
        upsertBootstrapUser(managerEmail, managerPassword, managerName, UserRole.MANAGER);
    }

    private void upsertBootstrapUser(String email, String rawPassword, String name, UserRole role) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepDAO.findByEmail(normalizedEmail)
                .orElseGet(User::new);

        user.setEmail(normalizedEmail);
        user.setName(name.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setFlagged(false);
        user.setActive(true);

        User savedUser = userRepDAO.save(user);
        LOGGER.info("Bootstrapped dev {} account email={} id={}", role.name().toLowerCase(), savedUser.getEmail(), savedUser.getId());
    }
}
