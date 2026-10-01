package com.ga.medibook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DoctorRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Specialization ID is required")
    private Long specializationId;

    @NotBlank(message = "License number is required")
    @Size(
            max = 100,
            message = "License number must not exceed 100 characters"
    )
    private String licenseNumber;

    @Size(
            max = 2000,
            message = "Bio must not exceed 2000 characters"
    )
    private String bio;
}