package com.ga.medibook.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentWaitlistRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Preferred start date and time are required")
    @Future(message = "Preferred start date and time must be in the future")
    private LocalDateTime preferredStartDateTime;

    @NotNull(message = "Preferred end date and time are required")
    @Future(message = "Preferred end date and time must be in the future")
    private LocalDateTime preferredEndDateTime;

    @AssertTrue(message = "Preferred end date and time must be after start date and time")
    public boolean isTimeRangeValid() {
        if (preferredStartDateTime == null
                || preferredEndDateTime == null) {
            return true;
        }

        return preferredEndDateTime.isAfter(preferredStartDateTime);
    }
}
