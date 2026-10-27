package com.ga.medibook.service;

import com.ga.medibook.service.AppointmentEventService;
import com.ga.medibook.service.PersistentNotificationService;
import com.ga.medibook.service.WaitlistAllocationProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(WaitlistAllocationProcessor.class)
class WaitlistAllocationProcessorTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WaitlistAllocationProcessor allocationProcessor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AppointmentEventService appointmentEventService;

    @MockitoBean
    private PersistentNotificationService persistentNotificationService;

    @Test
    void concurrentJobsMustNotAllocateTheSameSlotTwice()
            throws Exception {

        // Arrange: create two different jobs for the same doctor and slot.
        TestFixture fixture = createTestFixture();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService workers = Executors.newFixedThreadPool(2);

        try {
            var workerA = workers.submit(() -> {
                ready.countDown();
                start.await();

                allocationProcessor.processJob(fixture.jobAId());
                return null;
            });

            var workerB = workers.submit(() -> {
                ready.countDown();
                start.await();

                allocationProcessor.processJob(fixture.jobBId());
                return null;
            });

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both workers should be ready"
            );

            // Make both workers compete at approximately the same time.
            start.countDown();

            workerA.get(15, TimeUnit.SECONDS);
            workerB.get(15, TimeUnit.SECONDS);

            // Assert: only one active appointment occupies this slot.
            Integer activeAppointments = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM appointments
                    WHERE doctor_id = ?
                      AND start_date_time = ?
                      AND end_date_time = ?
                      AND status <> 'CANCELLED'
                    """,
                    Integer.class,
                    fixture.doctorId(),
                    fixture.start(),
                    fixture.end()
            );

            assertEquals(
                    1,
                    activeAppointments,
                    "Concurrent jobs must not create duplicate active appointments"
            );

            // Assert: the waiting patient was matched to one appointment.
            Integer matchedEntries = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM appointment_waitlist
                    WHERE id = ?
                      AND status = 'MATCHED'
                      AND matched_appointment_id IS NOT NULL
                    """,
                    Integer.class,
                    fixture.waitlistId()
            );

            assertEquals(
                    1,
                    matchedEntries,
                    "The waitlist entry should be matched exactly once"
            );

            // Assert: both jobs finished processing.
            // One allocates the slot; the other finds it occupied.
            Integer completedJobs = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM background_jobs
                    WHERE id IN (?, ?)
                      AND status = 'COMPLETED'
                    """,
                    Integer.class,
                    fixture.jobAId(),
                    fixture.jobBId()
            );

            assertEquals(
                    2,
                    completedJobs,
                    "Both jobs should complete without duplicating the booking"
            );

        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    private TestFixture createTestFixture() {
        String unique = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start = now.plusDays(2)
                .withSecond(0)
                .withNano(0);

        LocalDateTime end = start.plusMinutes(30);

        Long patientUserId = jdbcTemplate.queryForObject("""
                INSERT INTO users
                    (email, password, role, status, email_verified,
                     created_at, updated_at)
                VALUES (?, ?, 'PATIENT', 'ACTIVE', TRUE, ?, ?)
                RETURNING id
                """,
                Long.class,
                "patient-" + unique + "@example.com",
                "test-password",
                now,
                now
        );

        Long doctorUserId = jdbcTemplate.queryForObject("""
                INSERT INTO users
                    (email, password, role, status, email_verified,
                     created_at, updated_at)
                VALUES (?, ?, 'DOCTOR', 'ACTIVE', TRUE, ?, ?)
                RETURNING id
                """,
                Long.class,
                "doctor-" + unique + "@example.com",
                "test-password",
                now,
                now
        );

        Long specializationId = jdbcTemplate.queryForObject("""
                INSERT INTO specializations
                    (name, created_at, updated_at)
                VALUES (?, ?, ?)
                RETURNING id
                """,
                Long.class,
                "Concurrency Test " + unique,
                now,
                now
        );

        Long doctorId = jdbcTemplate.queryForObject("""
                INSERT INTO doctors
                    (user_id, specialization_id, license_number,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                doctorUserId,
                specializationId,
                "TEST-" + unique,
                now,
                now
        );

        // Two different cancelled appointments for the same slot.
        Long appointmentAId = createCancelledAppointment(
                patientUserId, doctorId, start, end, now
        );

        Long appointmentBId = createCancelledAppointment(
                patientUserId, doctorId, start, end, now
        );

        Long jobAId = createPendingJob(appointmentAId, now);
        Long jobBId = createPendingJob(appointmentBId, now);

        // One waiting patient eligible for this exact slot.
        Long waitlistId = jdbcTemplate.queryForObject("""
                INSERT INTO appointment_waitlist
                    (patient_id, doctor_id,
                     preferred_start_date_time,
                     preferred_end_date_time,
                     status, matched_appointment_id,
                     created_at, updated_at)
                VALUES (?, ?, ?, ?, 'WAITING', NULL, ?, ?)
                RETURNING id
                """,
                Long.class,
                patientUserId,
                doctorId,
                start,
                end,
                now,
                now
        );

        return new TestFixture(
                doctorId,
                waitlistId,
                jobAId,
                jobBId,
                start,
                end
        );
    }

    private Long createCancelledAppointment(
            Long patientId,
            Long doctorId,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime now) {

        return jdbcTemplate.queryForObject("""
                INSERT INTO appointments
                    (patient_id, doctor_id, start_date_time,
                     end_date_time, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'CANCELLED', ?, ?)
                RETURNING id
                """,
                Long.class,
                patientId,
                doctorId,
                start,
                end,
                now,
                now
        );
    }

    private Long createPendingJob(
            Long appointmentId,
            LocalDateTime now) {

        return jdbcTemplate.queryForObject("""
                INSERT INTO background_jobs
                    (job_type, triggering_appointment_id, status,
                     attempt_count, scheduled_at, created_at, updated_at)
                VALUES ('WAITLIST_ALLOCATION', ?, 'PENDING', 0, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                appointmentId,
                now.minusSeconds(1),
                now,
                now
        );
    }

    private record TestFixture(
            Long doctorId,
            Long waitlistId,
            Long jobAId,
            Long jobBId,
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}
