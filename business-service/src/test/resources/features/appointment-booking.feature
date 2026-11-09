Feature: Appointment booking and waitlist management

  Scenario: Patient successfully books an available appointment
    Given a doctor has an available appointment slot
    When patient one books that appointment slot
    Then the appointment should be created successfully

  Scenario: Patient cannot book outside doctor availability
    Given a doctor has an available appointment slot
    When patient one books a slot outside the doctor's availability
    Then the booking request should be rejected

  Scenario: Patient cannot double book an appointment
    Given a doctor has an available appointment slot
    And patient one has already booked that appointment slot
    When patient two attempts to book the same appointment slot
    Then the booking request should be rejected

  Scenario: Patient successfully cancels their own appointment
    Given patient one has booked an appointment
    When patient one cancels their appointment
    Then the appointment should be cancelled

  Scenario: Patient cannot cancel another patient's appointment
    Given patient one has booked an appointment
    When patient two attempts to cancel patient one's appointment
    Then the cancellation request should be rejected
    And the appointment should remain active

  Scenario: Patient joins the appointment waitlist
    Given a doctor has an available appointment slot
    When patient one joins the waitlist for that slot
    Then patient one should have a waiting list entry

  Scenario: Waitlisted patient receives an appointment after cancellation
    Given patient one has booked an appointment
    And patient two is waiting for the same appointment slot
    When patient one cancels their appointment
    And the waitlist allocation is processed
    Then patient two should receive the cancelled appointment slot
