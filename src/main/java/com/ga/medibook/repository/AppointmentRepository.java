package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Appointment;
import com.ga.medibook.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByPatientId(
            Long patientId,
            Pageable pageable
    );

    Page<Appointment> findByDoctorId(
            Long doctorId,
            Pageable pageable
    );

    List<Appointment> findByDoctorIdAndStatus(
            Long doctorId,
            AppointmentStatus status
    );

    List<Appointment> findByPatientIdAndStatus(
            Long patientId,
            AppointmentStatus status
    );

    @Query("""
            SELECT a
            FROM Appointment a
            WHERE a.doctor.id = :doctorId
            AND a.status <> :cancelledStatus
            AND a.startDateTime < :endDateTime
            AND a.endDateTime > :startDateTime
            """)
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("cancelledStatus") AppointmentStatus cancelledStatus
    );
}