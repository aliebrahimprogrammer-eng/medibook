package com.ga.medibook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DoctorResponse {

    private Long id;

    private Long userId;

    private String email;

    private String firstName;

    private String lastName;

    private Long specializationId;

    private String specializationName;

    private String licenseNumber;

    private String bio;
}