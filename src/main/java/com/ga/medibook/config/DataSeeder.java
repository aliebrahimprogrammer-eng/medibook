
package com.ga.medibook.config;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.Specialization;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.AppointmentStatus;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.model.enums.UserStatus;
import com.ga.medibook.model.enums.WaitlistStatus;
import com.ga.medibook.repository.AppointmentRepository;
import com.ga.medibook.repository.AppointmentWaitlistRepository;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.SpecializationRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DoctorRepository doctorRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistRepository appointmentWaitlistRepository;
    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedSpecializations();
        seedUsers();
        seedDoctors();
        seedAvailability();
        seedWaitlistDemo();
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
                    "Experienced dermatologist providing skincare consultations."
            );
            doctorRepository.save(doctor3);
        }
    }

    private void seedAvailability() {
        Doctor doctor = findDoctor("doctor@medibook.com");
        Doctor doctor2 = findDoctor("doctor2@medibook.com");
        Doctor doctor3 = findDoctor("doctor3@medibook.com");

        if (!availabilityRepository.findByDoctorId(doctor.getId()).isEmpty()
                && !availabilityRepository.findByDoctorId(doctor2.getId()).isEmpty()
                && !availabilityRepository.findByDoctorId(doctor3.getId()).isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        createAvailability(doctor, today.atTime(9, 0), today.atTime(12, 0));
        createAvailability(doctor, today.atTime(14, 0), today.atTime(17, 0));

        createAvailability(doctor2, today.atTime(7, 0), today.atTime(20, 0));
        createAvailability(doctor3, today.atTime(12, 0), today.atTime(23, 0));

        createAvailability(doctor, tomorrow.atTime(9, 0), tomorrow.atTime(12, 0));
        createAvailability(doctor, tomorrow.atTime(14, 0), tomorrow.atTime(17, 0));

        createAvailability(doctor2, tomorrow.atTime(7, 0), tomorrow.atTime(20, 0));
        createAvailability(doctor3, tomorrow.atTime(12, 0), tomorrow.atTime(23, 0));
    }

    private void seedWaitlistDemo() {
        User hasan = findUser("patient@medibook.com");
        User taqi = findUser("patient2@medibook.com");
        User saad = findUser("patient3@medibook.com");
        Doctor doctor = findDoctor("doctor@medibook.com");

        LocalDateTime slotStart = LocalDate.now()
                .plusDays(1)
                .atTime(LocalTime.of(10, 0));

        LocalDateTime slotEnd = slotStart.plusHours(1);

        // Do not create another demo appointment if one already exists.
        List<Appointment> existingAppointments = appointmentRepository.findAll();

        Appointment existingDemoAppointment = existingAppointments.stream()
                .filter(appointment ->
                        appointment.getPatient().getId().equals(hasan.getId())
                                && appointment.getDoctor().getId().equals(doctor.getId())
                                && appointment.getStartDateTime().equals(slotStart)
                                && appointment.getEndDateTime().equals(slotEnd)
                )
                .findFirst()
                .orElse(null);

        if (existingDemoAppointment == null) {
            boolean slotOccupied = !appointmentRepository
                    .findOverlappingAppointments(
                            doctor.getId(),
                            slotStart,
                            slotEnd,
                            AppointmentStatus.CANCELLED
                    )
                    .isEmpty();

            if (slotOccupied) {
                log.warn(
                        "FIFO demo appointment was not seeded because tomorrow's 10:00 slot is occupied."
                );
                return;
            }

            Appointment appointment = new Appointment();
            appointment.setPatient(hasan);
            appointment.setDoctor(doctor);
            appointment.setStartDateTime(slotStart);
            appointment.setEndDateTime(slotEnd);
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setReason("FIFO waitlist demonstration");

            existingDemoAppointment = appointmentRepository.saveAndFlush(appointment);

            log.info(
                    "Seeded FIFO demo appointment with ID {}",
                    existingDemoAppointment.getId()
            );
        }

        // The demo requires an active, occupied slot to be waiting for cancellation.
        if (existingDemoAppointment.getStatus() == AppointmentStatus.CANCELLED) {
            log.warn(
                    "FIFO demo appointment {} is already cancelled. "
                            + "No new waitlist entries were seeded.",
                    existingDemoAppointment.getId()
            );
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        seedWaitlistEntry(
                taqi,
                doctor,
                slotStart,
                slotEnd,
                now.minusMinutes(10)
        );

        seedWaitlistEntry(
                saad,
                doctor,
                slotStart,
                slotEnd,
                now.minusMinutes(5)
        );
    }

    private void seedWaitlistEntry(
            User patient,
            Doctor doctor,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            LocalDateTime createdAt
    ) {
        boolean exists = appointmentWaitlistRepository.findAll().stream()
                .anyMatch(entry ->
                        entry.getPatient().getId().equals(patient.getId())
                                && entry.getDoctor().getId().equals(doctor.getId())
                                && entry.getPreferredStartDateTime().equals(slotStart)
                                && entry.getPreferredEndDateTime().equals(slotEnd)
                );

        if (exists) {
            return;
        }

        AppointmentWaitlist entry = new AppointmentWaitlist();
        entry.setPatient(patient);
        entry.setDoctor(doctor);
        entry.setPreferredStartDateTime(slotStart);
        entry.setPreferredEndDateTime(slotEnd);
        entry.setStatus(WaitlistStatus.WAITING);

        appointmentWaitlistRepository.saveAndFlush(entry);

        // @PrePersist sets createdAt to now; override it to establish FIFO order.
        entry.setCreatedAt(createdAt);
        appointmentWaitlistRepository.saveAndFlush(entry);

        log.info(
                "Seeded FIFO waitlist entry for patient {} with createdAt {}",
                patient.getEmail(),
                createdAt
        );
    }

    private Doctor findDoctor(String email) {
        User user = findUser(email);

        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Doctor profile not found for " + email
                ));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed user not found: " + email
                ));
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
