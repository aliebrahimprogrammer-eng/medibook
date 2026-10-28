
package com.ga.medibook.dto.response;

import com.ga.medibook.model.enums.WaitlistStatus;

import java.time.LocalDateTime;

public record AppointmentWaitlistResponse(
        Long id,
        Long patientId,
        Long doctorId,
        String doctorName,
        LocalDateTime preferredStartDateTime,
        LocalDateTime preferredEndDateTime,
        WaitlistStatus status,
        Long matchedAppointmentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
