package com.ga.businessservice.cucumber;

import com.ga.businessservice.entity.*;
import com.ga.businessservice.model.enums.AppointmentStatus;
import com.ga.businessservice.model.enums.WaitlistStatus;
import com.ga.businessservice.repository.*;
import com.ga.businessservice.service.WaitlistAllocationProcessor;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AppointmentBookingSteps {

    private static final AtomicInteger DAY_OFFSET = new AtomicInteger(3);

    @LocalServerPort
    private int port;

    @Autowired private ScenarioState state;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private SpecializationRepository specializationRepository;
    @Autowired private AvailabilityRepository availabilityRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private AppointmentWaitlistRepository waitlistRepository;
    @Autowired private BackgroundJobRepository backgroundJobRepository;
    @Autowired private WaitlistAllocationProcessor waitlistAllocationProcessor;

    private int lastStatusCode;

    private void configureRestAssured() {
        RestAssured.port = port;
    }

    private void createDoctorAndAvailability() {
        configureRestAssured();

        Specialization specialization = specializationRepository
                .findByName("Cucumber Test")
                .orElseGet(() -> {
                    Specialization s = new Specialization();
                    s.setName("Cucumber Test");
                    s.setDescription("Specialization for automated tests");
                    return specializationRepository.save(s);
                });

        Doctor doctor = doctorRepository.findByUserId(ScenarioState.DOCTOR_USER_ID)
                .orElseGet(() -> {
                    Doctor d = new Doctor();
                    d.setUserId(ScenarioState.DOCTOR_USER_ID);
                    d.setSpecialization(specialization);
                    d.setLicenseNumber("CUCUMBER-" + UUID.randomUUID());
                    d.setBio("Test doctor");
                    return doctorRepository.save(d);
                });

        state.setDoctorId(doctor.getId());

        int day = DAY_OFFSET.getAndIncrement();
        LocalDateTime start = LocalDateTime.now()
                .plusDays(day)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime end = start.plusMinutes(30);

        state.setSlotStart(start);
        state.setSlotEnd(end);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setStartDateTime(start.minusHours(1));
        availability.setEndDateTime(start.plusHours(2));
        availabilityRepository.save(availability);
    }

    private Response bookAppointment(String token, LocalDateTime start, LocalDateTime end) {
        return given()
                .port(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "doctorId": %d,
                          "startDateTime": "%s",
                          "endDateTime": "%s",
                          "reason": "Cucumber BDD test"
                        }
                        """.formatted(state.getDoctorId(), start, end))
                .when()
                .post("/api/appointments");
    }

    private Response joinWaitlist(String token) {
        return given()
                .port(port)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "doctorId": %d,
                          "preferredStartDateTime": "%s",
                          "preferredEndDateTime": "%s"
                        }
                        """.formatted(
                        state.getDoctorId(),
                        state.getSlotStart().minusMinutes(30),
                        state.getSlotEnd().plusMinutes(30)
                ))
                .when()
                .post("/api/waitlist");
    }

    private Appointment findAppointment(Long patientId) {
        return appointmentRepository.findAll().stream()
                .filter(a -> patientId.equals(a.getPatientId()))
                .filter(a -> state.getDoctorId().equals(a.getDoctor().getId()))
                .filter(a -> state.getSlotStart().equals(a.getStartDateTime()))
                .findFirst()
                .orElse(null);
    }

    @Given("a doctor has an available appointment slot")
    public void doctorHasAvailableSlot() {
        createDoctorAndAvailability();
    }

    @When("patient one books that appointment slot")
    public void patientOneBooksSlot() {
        Response response = bookAppointment(
                ScenarioState.PATIENT_ONE_TOKEN,
                state.getSlotStart(),
                state.getSlotEnd()
        );
        lastStatusCode = response.statusCode();
    }

    @Then("the appointment should be created successfully")
    public void appointmentCreatedSuccessfully() {
        assertEquals(201, lastStatusCode);
        Appointment appointment = findAppointment(ScenarioState.PATIENT_ONE_ID);
        assertNotNull(appointment, "Expected appointment was not saved");
        state.setAppointmentId(appointment.getId());
        assertEquals(AppointmentStatus.PENDING, appointment.getStatus());
    }

    @When("patient one books a slot outside the doctor's availability")
    public void patientBooksOutsideAvailability() {
        LocalDateTime outsideStart = state.getSlotStart().plusHours(4);
        lastStatusCode = bookAppointment(
                ScenarioState.PATIENT_ONE_TOKEN,
                outsideStart,
                outsideStart.plusMinutes(30)
        ).statusCode();
    }

    @Then("the booking request should be rejected")
    public void bookingRequestRejected() {
        assertTrue(lastStatusCode >= 400 && lastStatusCode < 500,
                "Expected a client error but received HTTP " + lastStatusCode);
    }

    @Given("patient one has already booked that appointment slot")
    public void patientOneAlreadyBooked() {
        Response response = bookAppointment(
                ScenarioState.PATIENT_ONE_TOKEN,
                state.getSlotStart(),
                state.getSlotEnd()
        );
        assertEquals(201, response.statusCode());
        Appointment appointment = findAppointment(ScenarioState.PATIENT_ONE_ID);
        assertNotNull(appointment);
        state.setAppointmentId(appointment.getId());
    }

    @When("patient two attempts to book the same appointment slot")
    public void patientTwoBooksSameSlot() {
        lastStatusCode = bookAppointment(
                ScenarioState.PATIENT_TWO_TOKEN,
                state.getSlotStart(),
                state.getSlotEnd()
        ).statusCode();
    }

    @Given("patient one has booked an appointment")
    public void patientOneHasBookedAppointment() {
        createDoctorAndAvailability();

        Response response = bookAppointment(
                ScenarioState.PATIENT_ONE_TOKEN,
                state.getSlotStart(),
                state.getSlotEnd()
        );

        assertEquals(201, response.statusCode(),
                "Could not create the appointment fixture");

        Appointment appointment = findAppointment(ScenarioState.PATIENT_ONE_ID);
        assertNotNull(appointment);
        state.setAppointmentId(appointment.getId());
    }

    @When("patient one cancels their appointment")
    public void patientOneCancelsAppointment() {
        lastStatusCode = given()
                .port(port)
                .header("Authorization", "Bearer " + ScenarioState.PATIENT_ONE_TOKEN)
                .when()
                .patch("/api/appointments/" + state.getAppointmentId() + "/cancel")
                .statusCode();
    }

    @Then("the appointment should be cancelled")
    public void appointmentShouldBeCancelled() {
        assertEquals(204, lastStatusCode);
        Appointment appointment = appointmentRepository.findById(state.getAppointmentId())
                .orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
    }

    @When("patient two attempts to cancel patient one's appointment")
    public void patientTwoCancelsAppointment() {
        lastStatusCode = given()
                .port(port)
                .header("Authorization", "Bearer " + ScenarioState.PATIENT_TWO_TOKEN)
                .when()
                .patch("/api/appointments/" + state.getAppointmentId() + "/cancel")
                .statusCode();
    }

    @Then("the cancellation request should be rejected")
    public void cancellationRequestRejected() {
        assertTrue(lastStatusCode >= 400 && lastStatusCode < 500,
                "Expected a client error but received HTTP " + lastStatusCode);
    }

    @Then("the appointment should remain active")
    public void appointmentShouldRemainActive() {
        Appointment appointment = appointmentRepository.findById(state.getAppointmentId())
                .orElseThrow();
        assertNotEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
    }

    @When("patient one joins the waitlist for that slot")
    public void patientOneJoinsWaitlist() {
        lastStatusCode = joinWaitlist(ScenarioState.PATIENT_ONE_TOKEN).statusCode();

        if (lastStatusCode == 201) {
            waitlistRepository.findAll().stream()
                    .filter(w -> ScenarioState.PATIENT_ONE_ID.equals(w.getPatientId()))
                    .filter(w -> state.getDoctorId().equals(w.getDoctor().getId()))
                    .filter(w -> state.getSlotStart().minusMinutes(30)
                            .equals(w.getPreferredStartDateTime()))
                    .findFirst()
                    .ifPresent(w -> state.setWaitlistId(w.getId()));
        }
    }

    @Then("patient one should have a waiting list entry")
    public void patientOneHasWaitingEntry() {
        assertEquals(201, lastStatusCode);
        assertNotNull(state.getWaitlistId(), "Waitlist entry was not saved");

        AppointmentWaitlist entry = waitlistRepository.findById(state.getWaitlistId())
                .orElseThrow();
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
    }

    @Given("patient two is waiting for the same appointment slot")
    public void patientTwoIsWaiting() {
        Response response = joinWaitlist(ScenarioState.PATIENT_TWO_TOKEN);
        assertEquals(201, response.statusCode(),
                "Could not create patient two's waitlist entry");
    }

    @When("the waitlist allocation is processed")
    public void waitlistAllocationProcessed() {
        BackgroundJob job = backgroundJobRepository.findAll().stream()
                .filter(j -> state.getAppointmentId().equals(
                        j.getTriggeringAppointment().getId()))
                .filter(j -> "WAITLIST_ALLOCATION".equals(j.getJobType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Waitlist allocation job not found"));

        waitlistAllocationProcessor.processJob(job.getId());
    }

    @Then("patient two should receive the cancelled appointment slot")
    public void patientTwoReceivesSlot() {
        Appointment allocated = appointmentRepository.findAll().stream()
                .filter(a -> ScenarioState.PATIENT_TWO_ID.equals(a.getPatientId()))
                .filter(a -> state.getDoctorId().equals(a.getDoctor().getId()))
                .filter(a -> state.getSlotStart().equals(a.getStartDateTime()))
                .findFirst()
                .orElse(null);

        assertNotNull(allocated, "Waitlisted patient did not receive the appointment");
        assertEquals(AppointmentStatus.PENDING, allocated.getStatus());

        AppointmentWaitlist entry = waitlistRepository.findAll().stream()
                .filter(w -> ScenarioState.PATIENT_TWO_ID.equals(w.getPatientId()))
                .filter(w -> state.getDoctorId().equals(w.getDoctor().getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(WaitlistStatus.MATCHED, entry.getStatus());
    }
}
