Feature: User Login

  As a registered user
  I want to log in to my account
  So that I can access MediBook securely

  Scenario: Successfully log in with valid credentials
    Given I have a registered and email-verified user
    When I submit the login request with valid credentials
    Then the login should be successful
    And the response should contain a JWT token