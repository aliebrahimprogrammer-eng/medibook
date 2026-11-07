package com.ga.businessservice.dto.response;

import com.ga.businessservice.model.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;

    private Long patientId;

    private Long doctorId;

    private String doctorName;

    private String patientName;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private AppointmentStatus status;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
