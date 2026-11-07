package com.ga.businessservice.service;

import com.ga.businessservice.dto.request.AvailabilityRequest;
import com.ga.businessservice.dto.response.AvailabilityResponse;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.exception.ResourceConflictException;
import com.ga.businessservice.entity.Availability;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.repository.AvailabilityRepository;
import com.ga.businessservice.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final DoctorRepository doctorRepository;
    private final UserServiceClient userServiceClient;
    private final AuditLogService auditLogService;

    @Transactional
    public AvailabilityResponse create(
            String doctorEmail,
            AvailabilityRequest request
    ) {
        if (!request.getEndDateTime()
                .isAfter(request.getStartDateTime())) {

            throw new IllegalArgumentException(
                    "End date and time must be after start date and time"
            );
        }

        InternalUserResponse doctorUser =
                userServiceClient.findByEmail(doctorEmail);

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.id()
        ).orElseThrow(() ->
                new IllegalArgumentException("Doctor profile not found")
        );

        List<Availability> existingAvailabilities =
                availabilityRepository.findByDoctorId(doctor.getId());

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
            throw new ResourceConflictException(
                    "Availability overlaps with an existing availability"
            );
        }

        Availability availability = new Availability();

        availability.setDoctor(doctor);
        availability.setStartDateTime(request.getStartDateTime());
        availability.setEndDateTime(request.getEndDateTime());

        Availability saved = availabilityRepository.save(availability);

        auditLogService.log(
                doctorUser,
                "CREATE_AVAILABILITY",
                "AVAILABILITY",
                saved.getId(),
                "A new availability id " + saved.getId()
                        + " created for doctor id " + doctorUser.id()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AvailabilityResponse> findByDoctor(
            Long doctorId,
            Pageable pageable
    ) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new IllegalArgumentException("Doctor not found");
        }

        return availabilityRepository
                .findByDoctorId(doctorId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void delete(
            Long id,
            String doctorEmail
    ) {
        InternalUserResponse doctorUser =
                userServiceClient.findByEmail(doctorEmail);

        Doctor doctor = doctorRepository.findByUserId(
                doctorUser.id()
        ).orElseThrow(() ->
                new IllegalArgumentException("Doctor profile not found")
        );

        Availability availability =
                availabilityRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Availability not found"
                                )
                        );

        if (!availability.getDoctor()
                .getId()
                .equals(doctor.getId())) {

            throw new IllegalArgumentException(
                    "You can only delete your own availability"
            );
        }

        availabilityRepository.delete(availability);

        auditLogService.log(
                doctorUser,
                "DELETE_AVAILABILITY",
                "AVAILABILITY",
                availability.getId(),
                "availability id " + availability.getId()
                        + " deleted for doctor id " + doctorUser.id()
        );
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