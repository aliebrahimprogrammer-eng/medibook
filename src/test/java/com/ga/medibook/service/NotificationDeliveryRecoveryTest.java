
package com.ga.medibook.service;

import com.ga.medibook.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationDeliveryRecoveryTest {

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRecoverOnlyStuckProcessingDeliveries() {
        // Arrange: create three deliveries with different states.
        LocalDateTime now = LocalDateTime.now();

        Long stuckDeliveryId = createDelivery(
                "PROCESSING",
                now.minusMinutes(6),
                now
        );

        Long recentDeliveryId = createDelivery(
                "PROCESSING",
                now.minusMinutes(1),
                now
        );

        Long sentDeliveryId = createDelivery(
                "SENT",
                now.minusMinutes(6),
                now
        );

        // Act: recover deliveries older than five minutes.
        int recovered = deliveryRepository.recoverStuckDeliveries(
                now.minusMinutes(5)
        );

        // Assert: only the stuck delivery was recovered.
        assertEquals(1, recovered);

        assertEquals("FAILED", findStatus(stuckDeliveryId));
        assertEquals("PROCESSING", findStatus(recentDeliveryId));
        assertEquals("SENT", findStatus(sentDeliveryId));
    }

    private Long createDelivery(
            String status,
            LocalDateTime lastAttemptAt,
            LocalDateTime now
    ) {
        String unique = UUID.randomUUID().toString();

        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO users
                    (email, password, role, status, email_verified,
                     created_at, updated_at)
                VALUES (?, ?, 'PATIENT', 'ACTIVE', TRUE, ?, ?)
                RETURNING id
                """,
                Long.class,
                "recovery-" + unique + "@example.com",
                "test-password",
                now,
                now
        );

        Long notificationId = jdbcTemplate.queryForObject("""
                INSERT INTO notifications
                    (user_id, notification_type, title, message,
                     status, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                RETURNING id
                """,
                Long.class,
                userId,
                "WAITLIST_MATCHED",
                "Recovery test",
                "Testing notification recovery.",
                now
        );

        return jdbcTemplate.queryForObject("""
                INSERT INTO notification_deliveries
                    (notification_id, channel, status, attempt_count,
                     last_attempt_at, created_at, updated_at)
                VALUES (?, 'EMAIL', ?, 1, ?, ?, ?)
                RETURNING id
                """,
                Long.class,
                notificationId,
                status,
                lastAttemptAt,
                now,
                now
        );
    }

    private String findStatus(Long deliveryId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM notification_deliveries WHERE id = ?",
                String.class,
                deliveryId
        );
    }
}
