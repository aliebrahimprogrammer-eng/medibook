Feature: User Registration

  As a new user
  I want to register an account
  So that I can access MediBook

  Scenario: Successfully register a new user
    Given I have valid registration details
    When I submit the registration request
    Then the registration should be successful
    And the response should contain the registered user's details