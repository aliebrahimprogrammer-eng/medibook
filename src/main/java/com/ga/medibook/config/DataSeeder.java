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
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.AvailabilityRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;
    private final AvailabilityRepository availabilityRepository;

    @Override
    public void run(String... args) {

        seedSpecializations();
        seedUsers();
        seedDoctors();
        seedAvailability();
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

    private void seedDoctors() {

        User doctorUser = userRepository.findByEmail("doctor@medibook.com")
                .orElseThrow();
        User doctorUser2 = userRepository.findByEmail("doctor2@medibook.com")
                .orElseThrow();
        User doctorUser3 = userRepository.findByEmail("doctor3@medibook.com")
                .orElseThrow();

        Specialization cardiology = specializationRepository
                .findByName("Cardiology")
                .orElseThrow();

        Specialization dermatology = specializationRepository
                .findByName("Dermatology")
                .orElseThrow();

        if (!doctorRepository.existsByUserId(doctorUser.getId())) {

            Doctor doctor = new Doctor();

            doctor.setUser(doctorUser);
            doctor.setSpecialization(cardiology);
            doctor.setLicenseNumber("DOC-10001");
            doctor.setBio(
                    "Experienced cardiologist providing cardiovascular consultations."
            );

            doctorRepository.save(doctor);
        }

        if (!doctorRepository.existsByUserId(doctorUser2.getId())) {

            Doctor doctor2 = new Doctor();

            doctor2.setUser(doctorUser2);
            doctor2.setSpecialization(cardiology);
            doctor2.setLicenseNumber("DOC-10002");
            doctor2.setBio(
                    "Experienced cardiologist providing cardiovascular consultations."
            );

            doctorRepository.save(doctor2);
        }
        if (!doctorRepository.existsByUserId(doctorUser3.getId())) {

            Doctor doctor3 = new Doctor();

            doctor3.setUser(doctorUser3);
            doctor3.setSpecialization(dermatology);
            doctor3.setLicenseNumber("DOC-10003");
            doctor3.setBio(
                    "Experienced dermatology providing skincare consultations."
            );

            doctorRepository.save(doctor3);
        }
    }

    private void seedAvailability() {

        Doctor doctor = doctorRepository.findByUserId(
                userRepository.findByEmail("doctor@medibook.com")
                        .orElseThrow()
                        .getId()
        ).orElseThrow();
        Doctor doctor2 = doctorRepository.findByUserId(
                userRepository.findByEmail("doctor2@medibook.com")
                        .orElseThrow()
                        .getId()
        ).orElseThrow();
        Doctor doctor3 = doctorRepository.findByUserId(
                userRepository.findByEmail("doctor3@medibook.com")
                        .orElseThrow()
                        .getId()
        ).orElseThrow();

        if (!availabilityRepository.findByDoctorId(doctor.getId()).isEmpty() &&
                !availabilityRepository.findByDoctorId(doctor2.getId()).isEmpty() &&
                !availabilityRepository.findByDoctorId(doctor3.getId()).isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        createAvailability(
                doctor,
                today.atTime(9, 0),
                today.atTime(12, 0)
        );

        createAvailability(
                doctor,
                today.atTime(14, 0),
                today.atTime(17, 0)
        );

        createAvailability(
                doctor2,
                today.atTime(7, 0),
                today.atTime(20, 0)
        );

        createAvailability(
                doctor3,
                today.atTime(12, 0),
                today.atTime(23, 0)
        );

        createAvailability(
                doctor,
                tomorrow.atTime(9, 0),
                tomorrow.atTime(12, 0)
        );

        createAvailability(
                doctor,
                tomorrow.atTime(14, 0),
                tomorrow.atTime(17, 0)
        );

        createAvailability(
                doctor2,
                tomorrow.atTime(7, 0),
                tomorrow.atTime(20, 0)
        );

        createAvailability(
                doctor3,
                tomorrow.atTime(12, 0),
                tomorrow.atTime(23, 0)
        );
    }

    private void createAvailability(
            Doctor doctor,
            LocalDateTime start,
            LocalDateTime end
    ) {
        Availability availability = new Availability();

        availability.setDoctor(doctor);
        availability.setStartDateTime(start);
        availability.setEndDateTime(end);

        availabilityRepository.save(availability);
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