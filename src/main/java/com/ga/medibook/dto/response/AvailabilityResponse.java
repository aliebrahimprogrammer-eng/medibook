package com.ga.medibook.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AvailabilityResponse {

    private Long id;
    private Long doctorId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}