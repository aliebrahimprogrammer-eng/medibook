package com.ga.businessservice.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentNotificationEvent(
        UUID eventId,
        String eventType,
        Long appointmentId,
        Long recipientUserId,
        String recipientEmail,
        String recipientRole,
        String emailSubject,
        String emailBody,
        String notificationMessage,
        LocalDateTime occurredAt
) {}
