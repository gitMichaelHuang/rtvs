Feature: Payment processing via RTP API

  Background:
    Given the system has been seeded with standard test accounts

  Scenario: Successful payment between two active accounts
    Given user "U100" is authenticated with role "ROLE_USER"
    When user "U100" sends a payment of "150.00" USD to "U200" with request id "PR-BDD-001"
    Then the response status is 200
    And the payment status is "COMPLETED"
    And a transactionId is present in the response

  Scenario: Payment rejected due to insufficient funds
    Given user "U300" is authenticated with role "ROLE_USER"
    When user "U300" sends a payment of "50.00" USD to "U200" with request id "PR-BDD-002"
    Then the response status is 200
    And the payment status is "REJECTED"
    And the reason code is "INSUFFICIENT_FUNDS"

  Scenario: Duplicate payment request returns idempotent result
    Given user "U100" is authenticated with role "ROLE_USER"
    And a payment of "50.00" USD to "U200" with request id "PR-BDD-IDEM" has already been processed
    When user "U100" sends a payment of "50.00" USD to "U200" with request id "PR-BDD-IDEM"
    Then the payment status is "COMPLETED"
    And the transactionId matches the original transaction

  Scenario: V2 endpoint returns processingMode and estimatedCompletionSeconds
    Given user "U100" is authenticated with role "ROLE_USER"
    When user "U100" sends a V2 payment of "100.00" USD to "U200" with request id "PR-BDD-V2" and processingMode "EXPRESS"
    Then the response status is 200
    And the payment status is "COMPLETED"
    And the V2 response contains processingMode "EXPRESS"
    And the V2 response contains estimatedCompletionSeconds 1
