package com.ga.medibook.service;

import com.ga.medibook.dto.request.DoctorRequest;
import com.ga.medibook.dto.response.DoctorResponse;
import com.ga.medibook.model.entity.Doctor;
import com.ga.medibook.model.entity.Specialization;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.model.entity.UserProfile;
import com.ga.medibook.model.enums.UserRole;
import com.ga.medibook.repository.DoctorRepository;
import com.ga.medibook.repository.SpecializationRepository;
import com.ga.medibook.repository.UserProfileRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SpecializationRepository specializationRepository;

    @Transactional
    public DoctorResponse create(DoctorRequest request) {

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
}