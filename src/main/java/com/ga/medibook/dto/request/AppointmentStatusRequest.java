package com.ga.medibook.dto.request;

import com.ga.medibook.model.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentStatusRequest {

    @NotNull(message = "Appointment status is required")
    private AppointmentStatus status;
}