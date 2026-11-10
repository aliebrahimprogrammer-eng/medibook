package com.ga.userservice.config;

import com.ga.userservice.model.entity.User;
import com.ga.userservice.model.entity.UserProfile;
import com.ga.userservice.model.enums.UserRole;
import com.ga.userservice.model.enums.UserStatus;
import com.ga.userservice.repository.UserRepository;
import com.ga.userservice.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("Starting User Service data seeding...");

        createUserIfNotExists(
                adminEmail,
                adminPassword,
                UserRole.ADMIN,
                "Admin",
                "User"
        );

        createUserIfNotExists(
                "patient@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Hasan",
                "Patient"
        );

        createUserIfNotExists(
                "patient2@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Taqi",
                "Patient"
        );

        createUserIfNotExists(
                "patient3@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Saad",
                "Patient"
        );

        createUserIfNotExists(
                "doctor@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Yahya",
                "Doctor"
        );

        createUserIfNotExists(
                "doctor2@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Mariam",
                "Doctor"
        );

        createUserIfNotExists(
                "doctor3@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Sarah",
                "Doctor"
        );

        log.info("User Service data seeding completed.");
    }

    private void createUserIfNotExists(
            String email,
            String password,
            UserRole role,
            String firstName,
            String lastName
    ) {

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User newUser = new User();

                    newUser.setEmail(email);
                    newUser.setPassword(
                            passwordEncoder.encode(password)
                    );
                    newUser.setRole(role);
                    newUser.setStatus(UserStatus.ACTIVE);
                    newUser.setEmailVerified(true);

                    User savedUser = userRepository.save(newUser);

                    log.info("Created user: {}", email);

                    return savedUser;
                });

        // Also repair missing profiles for users that already exist.
        if (!userProfileRepository.findByUserId(user.getId()).isPresent()) {

            UserProfile profile = new UserProfile();

            profile.setUser(user);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);

            userProfileRepository.save(profile);

            log.info("Created profile for: {}", email);
        }
    }
}