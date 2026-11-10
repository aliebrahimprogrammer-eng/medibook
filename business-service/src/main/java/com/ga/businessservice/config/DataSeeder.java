package com.ga.businessservice.config;

import com.ga.businessservice.entity.Appointment;
import com.ga.businessservice.entity.AppointmentWaitlist;
import com.ga.businessservice.entity.Availability;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.entity.Specialization;

import com.ga.businessservice.model.enums.AppointmentStatus;
import com.ga.businessservice.model.enums.WaitlistStatus;

import com.ga.businessservice.repository.AppointmentRepository;
import com.ga.businessservice.repository.AppointmentWaitlistRepository;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
import com.ga.businessservice.repository.SpecializationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;

    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentWaitlistRepository appointmentWaitlistRepository;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("Starting Business Service data seeding...");

        seedSpecializations();

        // User Service must seed its users first.
        if (!usersExist()) {
            log.warn(
                    "Demo users not found. Seed User Service first, "
                            + "then restart Business Service."
            );
            return;
        }

        seedDoctors();
        seedAvailability();
        seedWaitlistDemo();

        log.info("Business Service data seeding completed.");
    }

    // --------------------------------------------------
    // SPECIALIZATIONS
    // --------------------------------------------------

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

    private void createSpecializationIfNotExists(
            String name,
            String description
    ) {

        if (!specializationRepository.existsByName(name)) {

            Specialization specialization = new Specialization();

            specialization.setName(name);
            specialization.setDescription(description);

            specializationRepository.save(specialization);

            log.info("Created specialization: {}", name);
        }
    }

    // --------------------------------------------------
    // DOCTORS
    // --------------------------------------------------

    private void seedDoctors() {

        Long yahyaId = findUserId("doctor@medibook.com");
        Long mariamId = findUserId("doctor2@medibook.com");
        Long sarahId = findUserId("doctor3@medibook.com");

        Specialization cardiology = findSpecialization("Cardiology");
        Specialization dermatology = findSpecialization("Dermatology");

        createDoctorIfNotExists(
                yahyaId,
                cardiology,
                "DOC-10001",
                "Experienced cardiologist providing cardiovascular consultations."
        );

        createDoctorIfNotExists(
                mariamId,
                cardiology,
                "DOC-10002",
                "Experienced cardiologist providing cardiovascular consultations."
        );

        createDoctorIfNotExists(
                sarahId,
                dermatology,
                "DOC-10003",
                "Experienced dermatologist providing skincare consultations."
        );
    }

    private void createDoctorIfNotExists(
            Long userId,
            Specialization specialization,
            String licenseNumber,
            String bio
    ) {

        if (doctorRepository.existsByUserId(userId)) {
            return;
        }

        Doctor doctor = new Doctor();

        doctor.setUserId(userId);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(licenseNumber);
        doctor.setBio(bio);

        doctorRepository.save(doctor);

        log.info("Created doctor profile for user ID: {}", userId);
    }

    // --------------------------------------------------
    // AVAILABILITY
    // --------------------------------------------------

    private void seedAvailability() {

        Doctor yahya = findDoctor("doctor@medibook.com");
        Doctor mariam = findDoctor("doctor2@medibook.com");
        Doctor sarah = findDoctor("doctor3@medibook.com");

        // Avoid creating duplicate availability records.
        if (!availabilityRepository.findByDoctorId(yahya.getId()).isEmpty()
                && !availabilityRepository.findByDoctorId(mariam.getId()).isEmpty()
                && !availabilityRepository.findByDoctorId(sarah.getId()).isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        createAvailabilityIfEmpty(
                yahya,
                today.atTime(9, 0),
                today.atTime(12, 0)
        );

        createAvailabilityIfEmpty(
                yahya,
                today.atTime(14, 0),
                today.atTime(17, 0)
        );

        createAvailabilityIfEmpty(
                mariam,
                today.atTime(7, 0),
                today.atTime(20, 0)
        );

        createAvailabilityIfEmpty(
                sarah,
                today.atTime(12, 0),
                today.atTime(23, 0)
        );

        createAvailabilityIfEmpty(
                yahya,
                tomorrow.atTime(9, 0),
                tomorrow.atTime(12, 0)
        );

        createAvailabilityIfEmpty(
                yahya,
                tomorrow.atTime(14, 0),
                tomorrow.atTime(17, 0)
        );

        createAvailabilityIfEmpty(
                mariam,
                tomorrow.atTime(7, 0),
                tomorrow.atTime(20, 0)
        );

        createAvailabilityIfEmpty(
                sarah,
                tomorrow.atTime(12, 0),
                tomorrow.atTime(23, 0)
        );
    }

    private void createAvailabilityIfEmpty(
            Doctor doctor,
            LocalDateTime start,
            LocalDateTime end
    ) {

        if (!availabilityRepository.findByDoctorId(doctor.getId()).isEmpty()) {
            return;
        }

        Availability availability = new Availability();

        availability.setDoctor(doctor);
        availability.setStartDateTime(start);
        availability.setEndDateTime(end);

        availabilityRepository.save(availability);
    }

    // --------------------------------------------------
    // FIFO WAITLIST DEMONSTRATION
    // --------------------------------------------------

    private void seedWaitlistDemo() {

        Long hasanId = findUserId("patient@medibook.com");
        Long taqiId = findUserId("patient2@medibook.com");
        Long saadId = findUserId("patient3@medibook.com");

        Doctor doctor = findDoctor("doctor@medibook.com");

        LocalDateTime slotStart = LocalDate.now()
                .plusDays(1)
                .atTime(LocalTime.of(10, 0));

        LocalDateTime slotEnd = slotStart.plusHours(1);

        List<Appointment> existingAppointments =
                appointmentRepository.findAll();

        Appointment existingDemoAppointment =
                existingAppointments.stream()
                        .filter(appointment ->
                                appointment.getPatientId().equals(hasanId)
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
                        "FIFO demo appointment was not seeded because the slot is occupied."
                );
                return;
            }

            Appointment appointment = new Appointment();

            appointment.setPatientId(hasanId);
            appointment.setDoctor(doctor);
            appointment.setStartDateTime(slotStart);
            appointment.setEndDateTime(slotEnd);
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setReason("FIFO waitlist demonstration");

            existingDemoAppointment =
                    appointmentRepository.saveAndFlush(appointment);

            log.info(
                    "Seeded FIFO demo appointment with ID {}",
                    existingDemoAppointment.getId()
            );
        }

        if (existingDemoAppointment.getStatus() == AppointmentStatus.CANCELLED) {
            log.warn("Demo appointment is already cancelled.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        seedWaitlistEntry(
                taqiId,
                doctor,
                slotStart,
                slotEnd,
                now.minusMinutes(10)
        );

        seedWaitlistEntry(
                saadId,
                doctor,
                slotStart,
                slotEnd,
                now.minusMinutes(5)
        );
    }

    private void seedWaitlistEntry(
            Long patientId,
            Doctor doctor,
            LocalDateTime slotStart,
            LocalDateTime slotEnd,
            LocalDateTime createdAt
    ) {

        boolean exists = appointmentWaitlistRepository.findAll().stream()
                .anyMatch(entry ->
                        entry.getPatientId().equals(patientId)
                                && entry.getDoctor().getId().equals(doctor.getId())
                                && entry.getPreferredStartDateTime().equals(slotStart)
                                && entry.getPreferredEndDateTime().equals(slotEnd)
                );

        if (exists) {
            return;
        }

        AppointmentWaitlist entry = new AppointmentWaitlist();

        entry.setPatientId(patientId);
        entry.setDoctor(doctor);
        entry.setPreferredStartDateTime(slotStart);
        entry.setPreferredEndDateTime(slotEnd);
        entry.setStatus(WaitlistStatus.WAITING);

        appointmentWaitlistRepository.saveAndFlush(entry);

        // Preserve FIFO order for the demonstration.
        entry.setCreatedAt(createdAt);

        appointmentWaitlistRepository.saveAndFlush(entry);

        log.info(
                "Seeded FIFO waitlist entry for patient ID {}",
                patientId
        );
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private boolean usersExist() {

        return findUserIdOrNull("doctor@medibook.com") != null
                && findUserIdOrNull("patient@medibook.com") != null
                && findUserIdOrNull("patient2@medibook.com") != null
                && findUserIdOrNull("patient3@medibook.com") != null;
    }

    private Long findUserId(String email) {

        Long userId = findUserIdOrNull(email);

        if (userId == null) {
            throw new IllegalStateException(
                    "User not found: " + email
            );
        }

        return userId;
    }

    private Long findUserIdOrNull(String email) {

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email = ?",
                    Long.class,
                    email
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private Specialization findSpecialization(String name) {

        return specializationRepository.findByName(name)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Specialization not found: " + name
                        )
                );
    }

    private Doctor findDoctor(String email) {

        Long userId = findUserId(email);

        return doctorRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Doctor profile not found for: " + email
                        )
                );
    }
}