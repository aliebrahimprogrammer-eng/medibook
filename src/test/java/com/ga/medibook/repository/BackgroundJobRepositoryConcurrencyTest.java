package com.ga.medibook.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BackgroundJobConcurrencyTest {

    @Autowired
    private BackgroundJobRepository backgroundJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void onlyOneWorkerCanClaimTheSamePendingJob() throws Exception {
        // Arrange: create a real job in the test database.
        Long jobId = createPendingJob();

        LocalDateTime claimTime = LocalDateTime.now();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService workers = Executors.newFixedThreadPool(2);

        try {
            var workerA = workers.submit(() -> {
                ready.countDown();
                start.await();

                return transactionTemplate.execute(transaction ->
                        backgroundJobRepository.claimPendingJob(
                                jobId, claimTime
                        )
                );
            });

            var workerB = workers.submit(() -> {
                ready.countDown();
                start.await();

                return transactionTemplate.execute(transaction ->
                        backgroundJobRepository.claimPendingJob(
                                jobId, claimTime
                        )
                );
            });

            // Wait until both workers are ready.
            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both workers should be ready"
            );

            // Release both workers to compete for the same job.
            start.countDown();

            int resultA = workerA.get(10, TimeUnit.SECONDS);
            int resultB = workerB.get(10, TimeUnit.SECONDS);

            // Assert: exactly one worker claimed the job.
            assertEquals(
                    1,
                    resultA + resultB,
                    "Exactly one worker should claim the job"
            );

            assertTrue(
                    (resultA == 1 && resultB == 0)
                            || (resultA == 0 && resultB == 1),
                    "One worker should succeed and the other should fail"
            );

            // Verify the persisted database state.
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM background_jobs WHERE id = ?",
                    String.class,
                    jobId
            );

            Integer attemptCount = jdbcTemplate.queryForObject(
                    "SELECT attempt_count FROM background_jobs WHERE id = ?",
                    Integer.class,
                    jobId
            );

            assertEquals("PROCESSING", status);
            assertEquals(1, attemptCount);

        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    private Long createPendingJob() {
        String unique = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // Create a patient user.
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

        // Create a doctor user.
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
                "Test Specialization " + unique,
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

        Long appointmentId = jdbcTemplate.queryForObject("""
                INSERT INTO appointments
                    (patient_id, doctor_id, start_date_time,
                     end_date_time, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'CANCELLED', ?, ?)
                RETURNING id
                """,
                Long.class,
                patientUserId,
                doctorId,
                now.plusDays(1),
                now.plusDays(1).plusMinutes(30),
                now,
                now
        );

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
}
