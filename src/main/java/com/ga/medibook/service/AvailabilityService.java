package com.ga.medibook.service;

import com.ga.medibook.dto.request.AvailabilityRequest;
import com.ga.medibook.dto.response.AvailabilityResponse;
import com.ga.medibook.model.entity.Availability;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.repository.AvailabilityRepository;
import com.ga.medibook.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public AvailabilityResponse create(
            AvailabilityRequest request
    ) {

        if (!request.getEndDateTime()
                .isAfter(request.getStartDateTime())) {

            throw new IllegalArgumentException(
                    "End date and time must be after start date and time"
            );
        }

        Doctor doctor = doctorRepository.findById(
                request.getDoctorId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Doctor not found"
                )
        );

        List<Availability> existingAvailabilities =
                availabilityRepository.findByDoctorId(
                        doctor.getId()
                );

        boolean overlaps = existingAvailabilities
                .stream()
                .anyMatch(existing ->
                        existing.getStartDateTime()
                                .isBefore(request.getEndDateTime())
                                &&
                                existing.getEndDateTime()
                                        .isAfter(request.getStartDateTime())
                );

        if (overlaps) {
            throw new IllegalArgumentException(
                    "Availability overlaps with an existing availability"
            );
        }

        Availability availability = new Availability();

        availability.setDoctor(doctor);
        availability.setStartDateTime(
                request.getStartDateTime()
        );
        availability.setEndDateTime(
                request.getEndDateTime()
        );

        Availability saved =
                availabilityRepository.save(availability);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> findByDoctor(
            Long doctorId
    ) {

        if (!doctorRepository.existsById(doctorId)) {
            throw new IllegalArgumentException(
                    "Doctor not found"
            );
        }

        return availabilityRepository
                .findByDoctorId(doctorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {

        Availability availability =
                availabilityRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Availability not found"
                                )
                        );

        availabilityRepository.delete(availability);
    }

    private AvailabilityResponse toResponse(
            Availability availability
    ) {

        return new AvailabilityResponse(
                availability.getId(),
                availability.getDoctor().getId(),
                availability.getStartDateTime(),
                availability.getEndDateTime()
        );
    }
}