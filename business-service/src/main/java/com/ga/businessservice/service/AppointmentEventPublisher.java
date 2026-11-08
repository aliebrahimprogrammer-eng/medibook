package com.ga.businessservice.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import com.ga.businessservice.dto.event.AppointmentNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private static final String TOPIC = "appointment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(AppointmentNotificationEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    TOPIC,
                    String.valueOf(event.appointmentId()),
                    message
            );
        } catch (JacksonException  exception) {
            throw new IllegalStateException(
                    "Failed to serialize appointment notification event",
                    exception
            );
        }
    }
}
