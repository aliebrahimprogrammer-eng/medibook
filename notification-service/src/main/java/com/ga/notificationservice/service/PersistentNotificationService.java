
package com.ga.notificationservice.service;

import com.ga.notificationservice.model.entity.Notification;
import com.ga.notificationservice.model.entity.NotificationDelivery;
import com.ga.notificationservice.repository.NotificationDeliveryRepository;
import com.ga.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

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
            UUID sourceEventId,
            Long userId,
            String recipientEmail,
            Long appointmentId,
            Long waitlistEntryId,
            String notificationType,
            String title,
            String message,
            String emailSubject,
            String emailBody
    ) {
        if (sourceEventId != null &&
                notificationRepository.existsBySourceEventId(sourceEventId)) {
            return null;
        }
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

        if ((emailSubject == null) != (emailBody == null)) {
            throw new IllegalArgumentException(
                    "Email subject and body must either both be provided or both be null"
            );
        }

        if (emailSubject != null &&
                (emailSubject.isBlank() || emailBody.isBlank())) {
            throw new IllegalArgumentException(
                    "Email subject and body cannot be blank"
            );
        }

        Notification notification = new Notification();
        notification.setSourceEventId(sourceEventId);
        notification.setUserId(userId);
        notification.setRecipientEmail(recipientEmail);
        notification.setAppointmentId(appointmentId);
        notification.setWaitlistEntryId(waitlistEntryId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEmailSubject(emailSubject);
        notification.setEmailBody(emailBody);
        notification.setStatus(NOTIFICATION_PENDING);

        Notification savedNotification =
                notificationRepository.save(notification);

        // Create an email delivery only when email content exists.
        if (emailSubject != null) {
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
        }

        return savedNotification;
    }
}
