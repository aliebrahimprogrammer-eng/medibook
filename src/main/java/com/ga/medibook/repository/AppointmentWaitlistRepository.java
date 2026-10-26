
package com.ga.medibook.repository;

import com.ga.medibook.model.entity.AppointmentWaitlist;
import com.ga.medibook.model.enums.WaitlistStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentWaitlistRepository
        extends JpaRepository<AppointmentWaitlist, Long> {

    List<AppointmentWaitlist> findByPatientIdOrderByCreatedAtDesc(
            Long patientId
    );

    List<AppointmentWaitlist> findByDoctorIdAndStatusOrderByCreatedAtAsc(
            Long doctorId,
            WaitlistStatus status,
            Pageable pageable
    );

    boolean existsByPatientIdAndDoctorIdAndPreferredStartDateTimeAndPreferredEndDateTimeAndStatus(
            Long patientId,
            Long doctorId,
            LocalDateTime preferredStartDateTime,
            LocalDateTime preferredEndDateTime,
            WaitlistStatus status
    );

    long countByDoctorIdAndStatus(
            Long doctorId,
            WaitlistStatus status
    );
}
