
package com.ga.notificationservice.repository;

import com.ga.notificationservice.model.entity.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationIdOrderByCreatedAtDesc(
            Long notificationId
    );

    List<NotificationDelivery> findByStatusOrderByCreatedAtAsc(
            String status,
            Pageable pageable
    );

    /**
     * Atomically claims a delivery for one worker.
     * Returns 1 if claimed, or 0 if it is no longer eligible.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE NotificationDelivery d
        SET d.status = 'PROCESSING',
            d.attemptCount = d.attemptCount + 1,
            d.lastAttemptAt = :now,
            d.lastError = null
        WHERE d.id = :deliveryId
          AND d.status IN ('PENDING', 'FAILED')
          AND d.attemptCount < 3
        """)
    int claimPendingDelivery(
            @Param("deliveryId") Long deliveryId,
            @Param("now") LocalDateTime now
    );

    /**
     * Loads the delivery and its notification.
     */
    @EntityGraph(attributePaths = {"notification"})
    @Query("""
        SELECT d
        FROM NotificationDelivery d
        WHERE d.id = :deliveryId
        """)
    Optional<NotificationDelivery> findWithNotificationById(
            @Param("deliveryId") Long deliveryId
    );

    /**
     * Recovers deliveries stuck in PROCESSING.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE NotificationDelivery d
        SET d.status = 'FAILED',
            d.lastError = 'Delivery timed out while processing'
        WHERE d.status = 'PROCESSING'
          AND d.lastAttemptAt < :cutoff
        """)
    int recoverStuckDeliveries(
            @Param("cutoff") LocalDateTime cutoff
    );
}
