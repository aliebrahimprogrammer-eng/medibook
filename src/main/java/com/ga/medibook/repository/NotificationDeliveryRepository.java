
package com.ga.medibook.repository;

import com.ga.medibook.model.entity.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
