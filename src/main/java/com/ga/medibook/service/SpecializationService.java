package com.ga.medibook.service;

import com.ga.medibook.dto.request.SpecializationRequest;
import com.ga.medibook.dto.response.SpecializationResponse;
import com.ga.medibook.exception.ResourceConflictException;
import com.ga.medibook.model.entity.Specialization;
import com.ga.medibook.model.entity.User;
import com.ga.medibook.repository.SpecializationRepository;
import com.ga.medibook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public SpecializationResponse create(
            SpecializationRequest request,
            String adminEmail
    ) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        if (specializationRepository.existsByName(request.getName())) {
            throw new ResourceConflictException(
                    "Specialization already exists"
            );
        }

        Specialization specialization = new Specialization();

        specialization.setName(request.getName());
        specialization.setDescription(request.getDescription());

        Specialization saved =
                specializationRepository.save(specialization);

        auditLogService.log(
                admin,
                "CREATE_SPECIALIZATION",
                "SPECIALIZATION",
                specialization.getId(),
                "Specialization id " + specialization.getId() + " is created by user id " +  admin.getId()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SpecializationResponse> findAll() {

        return specializationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecializationResponse findById(Long id) {

        Specialization specialization =
                specializationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Specialization not found"
                                )
                        );

        return toResponse(specialization);
    }

    private SpecializationResponse toResponse(
            Specialization specialization
    ) {

        return new SpecializationResponse(
                specialization.getId(),
                specialization.getName(),
                specialization.getDescription(),
                specialization.getCreatedAt(),
                specialization.getUpdatedAt()
        );
    }
}