package com.mosiacstore.mosiac.infrastructure.config;


import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.domain.user.UserStatus;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Thêm admin user nếu chưa tồn tại
        if (!userRepository.existsByEmail("admin@vietshirt.com")) {
            User adminUser = User.builder()
                    .email("admin@vietshirt.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();

            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());

            userRepository.save(adminUser);
            System.out.println("Admin user created: admin@vietshirt.com / admin123");
        }
    }
}