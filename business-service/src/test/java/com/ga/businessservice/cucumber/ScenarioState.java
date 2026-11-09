package com.ga.businessservice.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ScenarioScope
public class ScenarioState {

    public static final Long PATIENT_ONE_ID = 1001L;
    public static final Long PATIENT_TWO_ID = 1002L;
    public static final Long DOCTOR_USER_ID = 2001L;

    public static final String PATIENT_ONE_EMAIL = "patient1@test.local";
    public static final String PATIENT_TWO_EMAIL = "patient2@test.local";
    public static final String DOCTOR_EMAIL = "doctor@test.local";

    public static final String PATIENT_ONE_TOKEN = "patient-one-token";
    public static final String PATIENT_TWO_TOKEN = "patient-two-token";
    public static final String DOCTOR_TOKEN = "doctor-token";

    private String activeToken = PATIENT_ONE_TOKEN;
    private Long doctorId;
    private Long appointmentId;
    private Long waitlistId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;

    public String getActiveToken() { return activeToken; }
    public void usePatientOne() { activeToken = PATIENT_ONE_TOKEN; }
    public void usePatientTwo() { activeToken = PATIENT_TWO_TOKEN; }
    public void useDoctor() { activeToken = DOCTOR_TOKEN; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public Long getWaitlistId() { return waitlistId; }
    public void setWaitlistId(Long waitlistId) { this.waitlistId = waitlistId; }

    public LocalDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalDateTime slotStart) { this.slotStart = slotStart; }

    public LocalDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalDateTime slotEnd) { this.slotEnd = slotEnd; }

    public void reset() {
        activeToken = PATIENT_ONE_TOKEN;
        doctorId = null;
        appointmentId = null;
        waitlistId = null;
        slotStart = null;
        slotEnd = null;
    }
}
