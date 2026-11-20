package com.secureauth.service;

import com.secureauth.entity.*;
import com.secureauth.repository.RoleRepository;
import com.secureauth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final boolean seedEnabled;
    private final String adminEmail;
    private final String adminPassword;
    private final String userEmail;
    private final String userPassword;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
        @Value("${app.seed.enabled:false}") boolean seedEnabled,
        @Value("${app.seed.admin-email:admin@secureauth.dev}") String adminEmail,
        @Value("${app.seed.admin-password:}") String adminPassword,
        @Value("${app.seed.user-email:alex@secureauth.dev}") String userEmail,
        @Value("${app.seed.user-password:}") String userPassword,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.seedEnabled = seedEnabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));
        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        if (!seedEnabled) {
            return;
        }

        if (!adminPassword.isBlank() && !userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User admin = new User();
            admin.setFullName("Elena Vance (Admin)");
            admin.setUsername("admin");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setRoles(Set.of(userRole, adminRole));
            userRepository.save(admin);
            log.info("Seeded development admin account: {}", adminEmail);
        }

        if (!userPassword.isBlank() && !userRepository.existsByEmailIgnoreCase(userEmail)) {
            User standardUser = new User();
            standardUser.setFullName("Alex Morgan");
            standardUser.setUsername("alex.morgan");
            standardUser.setEmail(userEmail.toLowerCase());
            standardUser.setPassword(passwordEncoder.encode(userPassword));
            standardUser.setAccountStatus(AccountStatus.ACTIVE);
            standardUser.setRoles(Set.of(userRole));
            userRepository.save(standardUser);
            log.info("Seeded development user account: {}", userEmail);
        }
    }
}
