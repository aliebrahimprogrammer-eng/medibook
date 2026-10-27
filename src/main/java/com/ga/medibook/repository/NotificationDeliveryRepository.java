
package com.ga.medibook.repository;

import com.ga.medibook.model.entity.NotificationDelivery;
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
     *
     * Returns 1 if this worker claimed it, or 0 if another worker
     * already claimed it or it is no longer eligible.
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
     * Load the delivery together with the notification and its user,
     * so they can be accessed after the repository transaction ends.
     */
    @EntityGraph(attributePaths = {"notification", "notification.user"})
    @Query("""
        SELECT d
        FROM NotificationDelivery d
        WHERE d.id = :deliveryId
        """)
    Optional<NotificationDelivery> findWithNotificationAndUserById(
            @Param("deliveryId") Long deliveryId
    );
}
