Feature: Invalid User Login

  As a user
  I want the system to reject invalid credentials
  So that unauthorized users cannot access MediBook

  Scenario: Reject login with an incorrect password
    Given I have a registered user
    When I submit the login request with an incorrect password
    Then the login should be rejected