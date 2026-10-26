package com.ga.medibook.service;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.entity.AppointmentEvent;
import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.repository.AppointmentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppointmentEventService {

    private final AppointmentEventRepository eventRepository;

    /**
     * Records an event associated with an appointment.
     *
     * @param appointment the appointment involved in the event
     * @param eventType a short identifier describing the event
     * @param details additional information about the event
     */
    @Transactional
    public AppointmentEvent recordAppointmentEvent(
            Appointment appointment,
            String eventType,
            String details
    ) {
        if (appointment == null) {
            throw new IllegalArgumentException(
                    "Appointment is required to record this event"
            );
        }

        return saveEvent(
                appointment,
                null,
                eventType,
                details
        );
    }

    /**
     * Records an event associated with a waitlist entry.
     *
     * @param waitlistEntry the waitlist entry involved in the event
     * @param eventType a short identifier describing the event
     * @param details additional information about the event
     */
    @Transactional
    public AppointmentEvent recordWaitlistEvent(
            AppointmentWaitlist waitlistEntry,
            String eventType,
            String details
    ) {
        if (waitlistEntry == null) {
            throw new IllegalArgumentException(
                    "Waitlist entry is required to record this event"
            );
        }

        return saveEvent(
                null,
                waitlistEntry,
                eventType,
                details
        );
    }

    private AppointmentEvent saveEvent(
            Appointment appointment,
            AppointmentWaitlist waitlistEntry,
            String eventType,
            String details
    ) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Event type is required"
            );
        }

        AppointmentEvent event = new AppointmentEvent();
        event.setAppointment(appointment);
        event.setWaitlistEntry(waitlistEntry);
        event.setEventType(eventType);
        event.setDetails(details);
        event.setOccurredAt(LocalDateTime.now());

        return eventRepository.save(event);
    }
}
