package com.don.ecommerce.config;

import com.don.ecommerce.model.User;
import com.don.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFirstName;
    private final String adminLastName;

    public AdminUserSeeder(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin.email:admin@example.com}") String adminEmail,
                           @Value("${app.seed.admin.password:AdminPassword123}") String adminPassword,
                           @Value("${app.seed.admin.firstName:Admin}") String adminFirstName,
                           @Value("${app.seed.admin.lastName:User}") String adminLastName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!userRepository.existsByEmail(adminEmail)) {
            String hashed = passwordEncoder.encode(adminPassword);
            User admin = new User(adminFirstName, adminLastName, adminEmail, hashed,
                    "", "", "", "", "", "");
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("Seeded admin user: " + adminEmail);
            System.out.println("Use this password to login (change after first login): [provided in app properties]");
        } else {
            userRepository.findByEmail(adminEmail).ifPresent(u -> {
                if (!"ROLE_ADMIN".equals(u.getRole())) {
                    u.setRole("ROLE_ADMIN");
                    userRepository.save(u);
                    System.out.println("Updated existing user to ROLE_ADMIN: " + adminEmail);
                }
            });
        }
    }
}

