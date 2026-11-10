Feature: Notification delivery management

  Scenario: Persist a valid notification event
    Given a valid user notification event exists
    When the notification event is consumed
    Then the notification and email delivery should be saved with pending status

  Scenario: Successfully process an email delivery
    Given a notification with a pending email delivery exists
    When the notification delivery is processed
    Then the delivery status should be sent

  Scenario: Recover a stuck notification delivery
    Given a notification delivery has been stuck processing for more than five minutes
    When the stuck delivery recovery process runs
    Then the delivery should be marked as failed with a timeout error
