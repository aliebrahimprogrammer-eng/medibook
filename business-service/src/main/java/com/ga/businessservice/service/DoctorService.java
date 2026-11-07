package com.ga.businessservice.service;

import com.ga.businessservice.dto.request.DoctorRequest;
import com.ga.businessservice.dto.request.DoctorUpdateRequest;
import com.ga.businessservice.dto.response.DoctorResponse;
import com.ga.businessservice.dto.response.InternalUserResponse;
import com.ga.businessservice.entity.Doctor;
import com.ga.businessservice.entity.Specialization;
import com.ga.businessservice.repository.DoctorRepository;
import com.ga.businessservice.repository.SpecializationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserServiceClient userServiceClient;
    private final SpecializationRepository specializationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public DoctorResponse create(
            DoctorRequest request,
            String adminEmail
    ) {
        InternalUserResponse admin =
                userServiceClient.findByEmail(adminEmail);

        InternalUserResponse user =
                userServiceClient.findById(request.getUserId());

        if (!"DOCTOR".equals(user.role())) {
            throw new IllegalArgumentException(
                    "User must have the DOCTOR role"
            );
        }

        if (doctorRepository.existsByUserId(user.id())) {
            throw new IllegalArgumentException(
                    "Doctor profile already exists for this user"
            );
        }

        Specialization specialization =
                specializationRepository.findById(
                        request.getSpecializationId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Specialization not found"
                        )
                );

        if (doctorRepository.existsByLicenseNumber(
                request.getLicenseNumber()
        )) {
            throw new IllegalArgumentException(
                    "License number is already registered"
            );
        }

        Doctor doctor = new Doctor();

        doctor.setUserId(user.id());
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setBio(request.getBio());

        Doctor savedDoctor = doctorRepository.save(doctor);

        auditLogService.log(
                admin,
                "CREATE_DOCTOR",
                "DOCTOR",
                savedDoctor.getId(),
                "A new doctor id " + savedDoctor.getId()
                        + " created by admin id " + admin.id()
        );

        return toResponse(savedDoctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse findByUserEmail(String email) {
        InternalUserResponse user =
                userServiceClient.findByEmail(email);

        if (!"DOCTOR".equals(user.role())) {
            throw new IllegalArgumentException(
                    "The authenticated user is not a doctor"
            );
        }

        Doctor doctor = doctorRepository.findByUserId(user.id())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor profile not found for this user"
                        )
                );

        return toResponse(doctor);
    }

    @Transactional(readOnly = true)
    public DoctorResponse findById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Doctor not found"
                        )
                );

        return toResponse(doctor);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        InternalUserResponse user =
                userServiceClient.findById(doctor.getUserId());

        Specialization specialization = doctor.getSpecialization();

        return new DoctorResponse(
                doctor.getId(),
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                specialization.getId(),
                specialization.getName(),
                doctor.getLicenseNumber(),
                doctor.getBio()
        );
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> searchDoctors(
            String name,
            String specialization,
            Pageable pageable
    ) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasSpecialization =
                specialization != null && !specialization.isBlank();

        if (!hasName && hasSpecialization) {
            return doctorRepository
                    .searchBySpecialization(specialization, pageable)
                    .map(this::toResponse);
        }

        if (!hasName && !hasSpecialization) {
            return doctorRepository.findAll(pageable)
                    .map(this::toResponse);
        }

        // User names belong to User Service, so filtering happens
        // after retrieving the doctor records.
        List<Doctor> matchingDoctors = doctorRepository.findAll()
                .stream()
                .filter(doctor -> {
                    if (hasSpecialization &&
                            !doctor.getSpecialization().getName()
                                    .equalsIgnoreCase(specialization)) {
                        return false;
                    }

                    InternalUserResponse user =
                            userServiceClient.findById(doctor.getUserId());

                    String fullName =
                            (user.firstName() + " " + user.lastName())
                                    .toLowerCase();

                    return fullName.contains(name.toLowerCase())
                            || user.firstName().toLowerCase()
                                    .contains(name.toLowerCase())
                            || user.lastName().toLowerCase()
                                    .contains(name.toLowerCase());
                })
                .toList();

        List<DoctorResponse> responses = matchingDoctors.stream()
                .map(this::toResponse)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());

        List<DoctorResponse> pageContent =
                start >= responses.size()
                        ? List.of()
                        : responses.subList(start, end);

        return new PageImpl<>(
                pageContent,
                pageable,
                responses.size()
        );
    }

    @Transactional
    public DoctorResponse update(
            Long doctorId,
            DoctorUpdateRequest request,
            String adminEmail
    ) {
        InternalUserResponse admin =
                userServiceClient.findByEmail(adminEmail);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Doctor not found")
                );

        Specialization specialization =
                specializationRepository.findById(
                        request.getSpecializationId()
                ).orElseThrow(() ->
                        new IllegalArgumentException("Specialization not found")
                );

        if (!doctor.getLicenseNumber().equals(request.getLicenseNumber())
                && doctorRepository.existsByLicenseNumber(
                request.getLicenseNumber()
        )) {
            throw new IllegalArgumentException(
                    "License number is already registered"
            );
        }

        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setBio(request.getBio());

        Doctor updatedDoctor = doctorRepository.save(doctor);

        auditLogService.log(
                admin,
                "UPDATE_DOCTOR",
                "DOCTOR",
                updatedDoctor.getId(),
                "Doctor id " + updatedDoctor.getId()
                        + " updated by admin id " + admin.id()
        );

        return toResponse(updatedDoctor);
    }
}