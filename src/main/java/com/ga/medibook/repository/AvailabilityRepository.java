package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityRepository
        extends JpaRepository<Availability, Long> {

    List<Availability> findByDoctorId(Long doctorId);

    Page<Availability> findByDoctorId(
            Long doctorId,
            Pageable pageable
    );

    List<Availability> findByDoctorIdAndStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqual(
            Long doctorId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}