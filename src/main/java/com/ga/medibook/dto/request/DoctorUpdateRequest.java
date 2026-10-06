package com.ga.medibook.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorUpdateRequest {

    @NotNull
    private Long specializationId;

    @NotNull
    @Size(max = 100)
    private String licenseNumber;

    private String bio;
}