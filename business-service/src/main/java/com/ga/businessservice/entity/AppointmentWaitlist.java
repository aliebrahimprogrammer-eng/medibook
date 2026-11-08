package com.ga.businessservice.entity;

import com.ga.businessservice.model.enums.WaitlistStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "appointment_waitlist",
        indexes = {
                @Index(
                        name = "idx_waitlist_doctor_status_created",
                        columnList = "doctor_id, status, created_at"
                ),
                @Index(
                        name = "idx_waitlist_patient_status",
                        columnList = "patient_id, status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AppointmentWaitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "preferred_start_date_time", nullable = false)
    private LocalDateTime preferredStartDateTime;

    @Column(name = "preferred_end_date_time", nullable = false)
    private LocalDateTime preferredEndDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_appointment_id", unique = true)
    private Appointment matchedAppointment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}



