package com.ga.medibook.config;

import com.ga.medibook.model.entity.Specialization;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.repository.SpecializationRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        seedSpecializations();
        seedUsers();
    }

    private void seedSpecializations() {

        createSpecializationIfNotExists(
                "Cardiology",
                "Diagnosis and treatment of heart-related conditions"
        );

        createSpecializationIfNotExists(
                "Dermatology",
                "Diagnosis and treatment of skin conditions"
        );

        createSpecializationIfNotExists(
                "Pediatrics",
                "Medical care for infants, children and adolescents"
        );

        createSpecializationIfNotExists(
                "General Medicine",
                "General medical consultation and primary care"
        );
    }

    private void seedUsers() {

        User admin = createUserIfNotExists(
                "admin@medibook.com",
                "Password123",
                UserRole.ADMIN,
                "Admin",
                "User"
        );

        User patient = createUserIfNotExists(
                "patient@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Hasan",
                "Patient"
        );

        User patient2 = createUserIfNotExists(
                "patient2@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Taqi",
                "Patient"
        );

        User patient3 = createUserIfNotExists(
                "patient3@medibook.com",
                "Password123",
                UserRole.PATIENT,
                "Saad",
                "Patient"
        );

        User doctorUser = createUserIfNotExists(
                "doctor@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Yahya",
                "Doctor"
        );

        User doctorUser2 = createUserIfNotExists(
                "doctor2@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Mariam",
                "Doctor"
        );

        User doctorUser3 = createUserIfNotExists(
                "doctor3@medibook.com",
                "Password123",
                UserRole.DOCTOR,
                "Sarah",
                "Doctor"
        );
    }

    private void createSpecializationIfNotExists(
            String name,
            String description
    ) {
        if (!specializationRepository.existsByName(name)) {

            Specialization specialization = new Specialization();

            specialization.setName(name);
            specialization.setDescription(description);

            specializationRepository.save(specialization);
        }
    }

    private User createUserIfNotExists(
            String email,
            String password,
            UserRole role,
            String firstName,
            String lastName
    ) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User user = new User();

                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRole(role);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setEmailVerified(true);

                    User savedUser = userRepository.save(user);

                    UserProfile profile = new UserProfile();

                    profile.setUser(savedUser);
                    profile.setFirstName(firstName);
                    profile.setLastName(lastName);

                    userProfileRepository.save(profile);

                    return savedUser;
                });
    }
}