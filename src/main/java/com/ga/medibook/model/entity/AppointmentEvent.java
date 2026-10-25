
package com.ga.medibook.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointment_events",
        indexes = {
                @Index(
                        name = "idx_appointment_events_appointment",
                        columnList = "appointment_id, occurred_at"
                ),
                @Index(
                        name = "idx_appointment_events_waitlist",
                        columnList = "waitlist_entry_id, occurred_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AppointmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waitlist_entry_id")
    private AppointmentWaitlist waitlistEntry;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
