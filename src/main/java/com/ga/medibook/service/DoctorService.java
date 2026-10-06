package com.ga.medibook.service;

import com.ga.medibook.dto.request.DoctorRequest;
import com.ga.medibook.dto.request.DoctorUpdateRequest;
import com.ga.medibook.dto.response.DoctorResponse;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.Specialization;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpecializationRepository specializationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public DoctorResponse create(
            DoctorRequest request,
            String adminEmail
    ) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        if (user.getRole() != UserRole.DOCTOR) {
            throw new IllegalArgumentException(
                    "User must have the DOCTOR role"
            );
        }

        if (doctorRepository.existsByUserId(user.getId())) {
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

        doctor.setUser(user);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setBio(request.getBio());

        Doctor savedDoctor = doctorRepository.save(doctor);

        auditLogService.log(
                admin,
                "CREATE_DOCTOR",
                "DOCTOR",
                doctor.getId(),
                "A new doctor id " + doctor.getId() + " created by admin id " +  admin.getId()
        );

        return toResponse(savedDoctor);
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

        User user = doctor.getUser();

        UserProfile profile =
                userProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User profile not found"
                                )
                        );

        Specialization specialization =
                doctor.getSpecialization();

        return new DoctorResponse(
                doctor.getId(),
                user.getId(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
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

        Page<Doctor> doctors;

        boolean hasName = name != null && !name.isBlank();
        boolean hasSpecialization =
                specialization != null && !specialization.isBlank();

        if (hasName && hasSpecialization) {

            doctors = doctorRepository.searchByNameAndSpecialization(
                    name,
                    specialization,
                    pageable
            );

        } else if (hasName) {

            doctors = doctorRepository.searchByName(
                    name,
                    pageable
            );

        } else if (hasSpecialization) {

            doctors = doctorRepository.searchBySpecialization(
                    specialization,
                    pageable
            );

        } else {

            doctors = doctorRepository.findAll(pageable);
        }

        return doctors.map(this::toResponse);
    }
    @Transactional
    public DoctorResponse update(
            Long doctorId,
            DoctorUpdateRequest request,
            String adminEmail
    ) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Admin user not found")
                );

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
                doctor.getId(),
                "Doctor id " + doctor.getId()
                        + " updated by admin id " + admin.getId()
        );

        return toResponse(updatedDoctor);
    }

}