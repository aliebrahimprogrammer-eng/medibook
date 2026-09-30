package com.ga.medibook.dto.response;

import com.ga.medibook.model.enums.AppointmentStatus;
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

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private AppointmentStatus status;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}