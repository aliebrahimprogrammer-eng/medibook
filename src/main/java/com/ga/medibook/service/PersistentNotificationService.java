package com.ga.medibook.service;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.model.entity.Notification;
import com.ga.medibook.model.entity.NotificationDelivery;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.repository.NotificationDeliveryRepository;
import com.ga.medibook.repository.NotificationRepository;
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
            User user,
            Appointment appointment,
            AppointmentWaitlist waitlistEntry,
            String notificationType,
            String title,
            String message
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "Notification recipient is required"
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

        // Save the in-app notification first.
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setAppointment(appointment);
        notification.setWaitlistEntry(waitlistEntry);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NOTIFICATION_PENDING);

        Notification savedNotification =
                notificationRepository.save(notification);

        // Create a durable record of the email delivery to be attempted.
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
