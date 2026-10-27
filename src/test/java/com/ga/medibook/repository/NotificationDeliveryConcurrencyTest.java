package com.ga.medibook.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
class NotificationDeliveryConcurrencyTest {

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void onlyOneWorkerCanClaimTheSamePendingDelivery() throws Exception {
        // Arrange: create a real pending notification delivery.
        Long deliveryId = createPendingDelivery();

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
                        notificationDeliveryRepository.claimPendingDelivery(
                                deliveryId,
                                claimTime
                        )
                );
            });

            var workerB = workers.submit(() -> {
                ready.countDown();
                start.await();

                return transactionTemplate.execute(transaction ->
                        notificationDeliveryRepository.claimPendingDelivery(
                                deliveryId,
                                claimTime
                        )
                );
            });

            // Wait until both workers are ready.
            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "Both workers should be ready"
            );

            // Let both workers compete for the same delivery.
            start.countDown();

            int resultA = workerA.get(10, TimeUnit.SECONDS);
            int resultB = workerB.get(10, TimeUnit.SECONDS);

            // Exactly one worker should claim the delivery.
            assertEquals(
                    1,
                    resultA + resultB,
                    "Exactly one worker should claim the delivery"
            );

            assertTrue(
                    (resultA == 1 && resultB == 0)
                            || (resultA == 0 && resultB == 1),
                    "One worker should succeed and the other should fail"
            );

            // Verify the persisted database state.
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM notification_deliveries WHERE id = ?",
                    String.class,
                    deliveryId
            );

            Integer attemptCount = jdbcTemplate.queryForObject(
                    "SELECT attempt_count FROM notification_deliveries WHERE id = ?",
                    Integer.class,
                    deliveryId
            );

            assertEquals("PROCESSING", status);
            assertEquals(1, attemptCount);

        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    private Long createPendingDelivery() {
        String unique = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // Create a patient user for the notification.
        Long patientUserId = jdbcTemplate.queryForObject("""
                INSERT INTO users
                    (email, password, role, status, email_verified,
                     created_at, updated_at)
                VALUES (?, ?, 'PATIENT', 'ACTIVE', TRUE, ?, ?)
                RETURNING id
                """,
                Long.class,
                "delivery-test-" + unique + "@example.com",
                "test-password",
                now,
                now
        );

        // Create the persistent notification.

        Long notificationId = jdbcTemplate.queryForObject("""
        INSERT INTO notifications
            (user_id, notification_type, title, message,
             status, created_at)
        VALUES (?, ?, ?, ?, 'PENDING', ?)
        RETURNING id
        """,
                Long.class,
                patientUserId,
                "WAITLIST_MATCHED",
                "Test notification",
                "A test appointment slot is available.",
                now
        );


        // Create the pending email delivery.
        return jdbcTemplate.queryForObject("""
                INSERT INTO notification_deliveries
                    (notification_id, channel, status, attempt_count,
                     created_at, updated_at)
                VALUES (?, 'EMAIL', 'PENDING', 0, ?, ?)
                RETURNING id
                """,
                Long.class,
                notificationId,
                now,
                now
        );
    }
}