Feature: Payment processing via RTP API

  Background:
    Given the system has been seeded with standard test accounts

  Scenario: Successful payment between two active accounts
    Given user "U100" is authenticated with role "ROLE_USER"
    When user "U100" sends a payment of "150.00" USD to "U200" with request id "PR-BDD-001"
    Then the response status is 200
    And the payment status is "COMPLETED"
    And a transactionId is present in the response


