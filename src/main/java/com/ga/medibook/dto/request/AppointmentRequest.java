package com.ga.medibook.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Start date and time are required")
    @Future(message = "Start date and time must be in the future")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date and time are required")
    @Future(message = "End date and time must be in the future")
    private LocalDateTime endDateTime;

    @Size(
            max = 500,
            message = "Reason must not exceed 500 characters"
    )
    private String reason;
}