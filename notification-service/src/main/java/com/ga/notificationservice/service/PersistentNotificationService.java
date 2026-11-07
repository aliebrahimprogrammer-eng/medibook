
package com.ga.notificationservice.service;

import com.ga.notificationservice.model.entity.Notification;
import com.ga.notificationservice.model.entity.NotificationDelivery;
import com.ga.notificationservice.repository.NotificationDeliveryRepository;
import com.ga.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PersistentNotificationService {

    private static final String NOTIFICATION_PENDING = "PENDING";
    private static final String EMAIL_CHANNEL = "EMAIL";
    private static final String DELIVERY_PENDING = "PENDING";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    @Transactional
    public Notification createNotification(
            Long userId,
            String recipientEmail,
            Long appointmentId,
            Long waitlistEntryId,
            String notificationType,
            String title,
            String message
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification recipient email is required"
            );
        }

        if (notificationType == null || notificationType.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification type is required"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification title is required"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message is required"
            );
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRecipientEmail(recipientEmail);
        notification.setAppointmentId(appointmentId);
        notification.setWaitlistEntryId(waitlistEntryId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NOTIFICATION_PENDING);

        Notification savedNotification =
                notificationRepository.save(notification);

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(savedNotification);
        delivery.setChannel(EMAIL_CHANNEL);
        delivery.setStatus(DELIVERY_PENDING);
        delivery.setAttemptCount(0);
        delivery.setLastAttemptAt(null);
        delivery.setDeliveredAt(null);
        delivery.setLastError(null);
        delivery.setCreatedAt(LocalDateTime.now());

        deliveryRepository.save(delivery);

        return savedNotification;
    }
}
