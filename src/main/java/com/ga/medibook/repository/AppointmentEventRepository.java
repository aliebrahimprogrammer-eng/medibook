
package com.ga.medibook.repository;

import com.ga.medibook.model.entity.AppointmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentEventRepository
        extends JpaRepository<AppointmentEvent, Long> {

    List<AppointmentEvent> findByAppointmentIdOrderByOccurredAtDesc(
            Long appointmentId
    );

    List<AppointmentEvent> findByWaitlistEntryIdOrderByOccurredAtDesc(
            Long waitlistEntryId
    );
}
