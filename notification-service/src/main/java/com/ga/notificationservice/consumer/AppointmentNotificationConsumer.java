
package com.ga.notificationservice.consumer;

import com.ga.notificationservice.event.AppointmentNotificationEvent;
import com.ga.notificationservice.service.PersistentNotificationService;
import com.ga.notificationservice.notification.SseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final PersistentNotificationService notificationService;
    private final SseNotificationService sseNotificationService;

    @KafkaListener(topics = "appointment-events")
    public void consume(String payload) {

        try {
            AppointmentNotificationEvent event =
                    objectMapper.readValue(
                            payload,
                            AppointmentNotificationEvent.class
                    );

            var notification = notificationService.createNotification(
                    event.eventId(),
                    event.recipientUserId(),
                    event.recipientEmail(),
                    event.appointmentId(),
                    null,
                    event.eventType(),
                    "Appointment update",
                    event.notificationMessage(),
                    event.emailSubject(),
                    event.emailBody()
            );

            // A null result means this event was already processed.
            if (notification == null) {
                log.info(
                        "Skipping duplicate appointment event: {}",
                        event.eventId()
                );
                return;
            }

            // Send the live update only after notification persistence succeeds.
            try {
                sseNotificationService.sendNotification(
                        event.recipientEmail(),
                        event.notificationMessage()
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "Could not send live notification for event {}",
                        event.eventId(),
                        exception
                );
            }

            log.info(
                    "Processed appointment notification event: {}",
                    event.eventId()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Failed to process appointment notification event",
                    exception
            );
            throw exception;
        }
    }
}
