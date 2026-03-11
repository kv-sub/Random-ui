# Gherkin BDD Scenarios
## Insurance Claim Submission System

**Version:** 1.3  
**Date:** March 2026  
**Framework:** Cucumber-JVM (backend) / Playwright (frontend E2E)

---

## Document History

| Version | Date       | Changes                                                                             |
|---------|------------|-------------------------------------------------------------------------------------|
| 1.0     | 2026-01-05 | Initial scenarios for US-01, US-02, US-12, US-13 (Sprint 1)                        |
| 1.1     | 2026-02-16 | Added scenarios for US-04 duplicate prevention (Sprint 4)                           |
| 1.2     | 2026-03-02 | Added scenarios for US-06 and US-07 claim history timeline (Sprint 5)               |
| 1.3     | 2026-03-30 | Added scenarios for US-11 audit trail; expanded edge-case tags (Sprint 7)           |

---

## Feature: Policy Lookup

```gherkin
Feature: Policy Lookup
  As an Admin
  I want to search for an insurance policy by policy number
  So that I can view policy details and coverage limits

  Background:
    Given the following policies exist in the system:
      | policyNumber | customerId | status   | effectiveDate | expiryDate  | coverageLimit |
      | POL-AB123    | 1001       | ACTIVE   | 2025-01-01    | 2027-12-31  | 50000.00      |
      | POL-XY999    | 1002       | EXPIRED  | 2020-01-01    | 2024-12-31  | 30000.00      |
    And the following coverages exist for policy "POL-AB123":
      | claimType | limitAmount | isActive |
      | MEDICAL   | 10000.00    | true     |
      | DENTAL    | 2000.00     | true     |
      | VISION    | 1000.00     | false    |

  Scenario: Successfully retrieve an active policy
    Given I am an authenticated Admin
    When I request the policy with number "POL-AB123"
    Then the response status is 200
    And the response contains policy details:
      | field          | value      |
      | policyNumber   | POL-AB123  |
      | status         | ACTIVE     |
      | customerId     | 1001       |
      | coverageLimit  | 50000.00   |
    And the coverageLimits map contains:
      | claimType | limit    |
      | MEDICAL   | 10000.00 |
      | DENTAL    | 2000.00  |
    And the coverageLimits map does NOT contain "VISION" (inactive coverage)

  Scenario: Retrieve a policy with non-active status
    Given I am an authenticated Admin
    When I request the policy with number "POL-XY999"
    Then the response status is 200
    And the response contains:
      | field  | value   |
      | status | EXPIRED |

  Scenario: Policy not found
    Given I am an authenticated Admin
    When I request the policy with number "POL-ZZ000"
    Then the response status is 404
    And the response error is "Policy Not Found"
    And the response message contains "POL-ZZ000"
```

---

## Feature: Claim Submission

```gherkin
Feature: Claim Submission
  As a Customer
  I want to submit an insurance claim against my active policy
  So that I can request reimbursement for a covered incident

  Background:
    Given the following active policy exists:
      | policyNumber | status | effectiveDate | expiryDate  |
      | POL-AB123    | ACTIVE | 2025-01-01    | 2027-12-31  |
    And the policy has the following active coverages:
      | claimType | limitAmount |
      | MEDICAL   | 10000.00    |
      | AUTO      | 20000.00    |
    And today's date is "2026-03-10"

  Scenario: Successfully submit a valid claim
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | POL-AB123                                            |
      | claimType    | MEDICAL                                              |
      | claimAmount  | 1500.00                                              |
      | incidentDate | 2026-03-08                                           |
      | description  | Medical treatment required after accident            |
    Then the response status is 201
    And the response contains:
      | field        | value     |
      | policyNumber | POL-AB123 |
      | claimType    | MEDICAL   |
      | claimAmount  | 1500.00   |
      | status       | SUBMITTED |
    And a ClaimHistory record with status "SUBMITTED" exists for the claim

  Scenario: Claim amount exceeds per-type coverage limit
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | POL-AB123                                            |
      | claimType    | MEDICAL                                              |
      | claimAmount  | 15000.00                                             |
      | incidentDate | 2026-03-08                                           |
      | description  | Expensive medical treatment exceeding limits         |
    Then the response status is 400
    And the response error is "Coverage Exceeded"
    And the response message contains "15000.00"
    And the response message contains "10000.00"

  Scenario: Policy is not active
    Given a policy "POL-XY999" with status "EXPIRED"
    When I submit a claim against policy "POL-XY999"
    Then the response status is 400
    And the response error is "Policy Inactive"
    And the response message contains "EXPIRED"

  Scenario: Claim type not covered by policy
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | POL-AB123    |
      | claimType    | VISION       |
      | claimAmount  | 500.00       |
      | incidentDate | 2026-03-08   |
      | description  | Eye examination and new prescription glasses needed  |
    Then the response status is 400
    And the response error is "Invalid Claim Type"
    And the response message contains "VISION"
    And the response message contains "POL-AB123"

  Scenario: Future incident date is rejected
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | POL-AB123                                            |
      | claimType    | MEDICAL                                              |
      | claimAmount  | 500.00                                               |
      | incidentDate | 2026-12-31                                           |
      | description  | Future dated incident should be rejected by system   |
    Then the response status is 400
    And the response error is "Validation Failed"
    And the response details contain a message about "incidentDate"

  Scenario: Description too short is rejected
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | POL-AB123   |
      | claimType    | MEDICAL     |
      | claimAmount  | 500.00      |
      | incidentDate | 2026-03-08  |
      | description  | Too short   |
    Then the response status is 400
    And the response error is "Validation Failed"
    And the response details contain a message about "description"

  Scenario: Invalid policy number format
    Given I am an authenticated Customer
    When I submit a claim with:
      | policyNumber | INVALID-FORMAT |
      | claimType    | MEDICAL        |
      | claimAmount  | 500.00         |
      | incidentDate | 2026-03-08     |
      | description  | Valid description that is long enough to pass     |
    Then the response status is 400
    And the response error is "Validation Failed"
    And the response details contain a message about "policyNumber"

  Scenario: Policy does not exist
    Given I am an authenticated Customer
    When I submit a claim against policy "POL-ZZ000"
    Then the response status is 404
    And the response error is "Policy Not Found"
```

---

## Feature: Duplicate Claim Prevention

```gherkin
Feature: Duplicate Claim Prevention
  As the System
  I want to detect and reject duplicate claims
  So that fraudulent or accidental resubmissions are prevented

  Background:
    Given the following active policy "POL-AB123" exists with MEDICAL coverage up to $10,000
    And today's date is "2026-03-10"

  Scenario: Duplicate claim within 24 hours is rejected
    Given a claim was submitted 2 hours ago with:
      | policyNumber | POL-AB123  |
      | claimType    | MEDICAL    |
      | incidentDate | 2026-03-08 |
    When I submit a new claim with the same policy, type, and incident date
    Then the response status is 409
    And the response error is "Duplicate Claim"
    And the response message contains "POL-AB123"
    And the response message contains "MEDICAL"
    And the response message contains "2026-03-08"

  Scenario: Same claim type with different incident date is accepted
    Given a claim was submitted 2 hours ago for MEDICAL on date "2026-03-08"
    When I submit a new MEDICAL claim for "POL-AB123" with incident date "2026-03-07"
    Then the response status is 201

  Scenario: Same incident date with different claim type is accepted
    Given a MEDICAL claim was submitted 2 hours ago for incident date "2026-03-08"
    When I submit an AUTO claim for "POL-AB123" with incident date "2026-03-08"
    Then the response status is 201

  Scenario: Same details submitted after 25 hours are accepted
    Given a claim was submitted 25 hours ago with:
      | policyNumber | POL-AB123  |
      | claimType    | MEDICAL    |
      | incidentDate | 2026-03-08 |
    When I submit the same claim again
    Then the response status is 201
```

---

## Feature: Claim Status Tracking

```gherkin
Feature: Claim Status Tracking
  As a Customer
  I want to track the status of my claim by its ID
  So that I know whether it has been approved, rejected, or is under review

  Background:
    Given a claim exists with:
      | claimId | policyNumber | claimType | status    |
      | 42      | POL-AB123    | MEDICAL   | SUBMITTED |

  Scenario: Successfully retrieve claim status
    Given I am an authenticated Customer
    When I request claim status for claim ID 42
    Then the response status is 200
    And the response contains:
      | field        | value     |
      | claimId      | 42        |
      | policyNumber | POL-AB123 |
      | claimType    | MEDICAL   |
      | status       | SUBMITTED |

  Scenario: Claim not found
    Given I am an authenticated Customer
    When I request claim status for claim ID 9999
    Then the response status is 404
    And the response error is "Claim Not Found"
    And the response message contains "9999"

  Scenario: Retrieve an approved claim
    Given a claim with ID 43 has been approved by an admin
    When I request claim status for claim ID 43
    Then the response status is 200
    And the response field "status" is "APPROVED"

  Scenario: Retrieve a rejected claim
    Given a claim with ID 44 has been rejected with notes "Outside policy dates"
    When I request claim status for claim ID 44
    Then the response status is 200
    And the response field "status" is "REJECTED"
```

---

## Feature: Claim History

```gherkin
Feature: Claim History (Audit Trail)
  As a Customer or Admin
  I want to view the full history of status changes for a claim
  So that I have a complete audit trail

  Background:
    Given a claim with ID 42 was submitted and subsequently approved

  Scenario: View history of an approved claim
    When I request the history for claim ID 42
    Then the response status is 200
    And the history contains 2 records
    And the first record (newest) has:
      | field         | value       |
      | status        | APPROVED    |
      | reviewerNotes | All docs OK |
    And the second record (oldest) has:
      | field         | value     |
      | status        | SUBMITTED |
      | reviewerNotes | null      |

  Scenario: View history of a newly submitted claim
    Given a claim with ID 50 was just submitted
    When I request the history for claim ID 50
    Then the response status is 200
    And the history contains exactly 1 record
    And that record has status "SUBMITTED" and null reviewerNotes

  Scenario: History for a non-existent claim returns 404
    When I request the history for claim ID 9999
    Then the response status is 404
    And the response error is "Claim Not Found"

  Scenario: History is ordered newest first
    Given claim 42 has transitions: SUBMITTED → IN_REVIEW → APPROVED
    When I request the history for claim ID 42
    Then the first record has status "APPROVED"
    And the last record has status "SUBMITTED"
```

---

## Feature: Claim Review (Admin)

```gherkin
Feature: Claim Review
  As an Admin
  I want to approve or reject a submitted claim
  So that claims are adjudicated with a documented decision

  Background:
    Given a claim with ID 42 exists with status "SUBMITTED"

  Scenario: Admin approves a claim
    Given I am an authenticated Admin
    When I review claim 42 with:
      | action        | APPROVE                              |
      | reviewerNotes | All documentation verified. Approved.|
    Then the response status is 200
    And the response field "status" is "APPROVED"
    And a ClaimHistory record with status "APPROVED" and the reviewer notes is created
    And the claim detail page shows the Approve/Reject buttons are hidden

  Scenario: Admin rejects a claim
    Given I am an authenticated Admin
    When I review claim 42 with:
      | action        | REJECT                              |
      | reviewerNotes | Incident occurred outside policy term|
    Then the response status is 200
    And the response field "status" is "REJECTED"
    And a ClaimHistory record with status "REJECTED" is created with the reviewer notes

  Scenario: Review action is missing (validation error)
    Given I am an authenticated Admin
    When I submit a review for claim 42 with no action field
    Then the response status is 400
    And the response error is "Validation Failed"

  Scenario: Review a non-existent claim
    Given I am an authenticated Admin
    When I review claim 9999 with action "APPROVE"
    Then the response status is 404
    And the response error is "Claim Not Found"

  Scenario: Review an already approved claim
    Given claim 42 has status "APPROVED"
    When I review claim 42 with action "REJECT"
    Then the claim status remains "APPROVED"

  Scenario: Admin approves an IN_REVIEW claim
    Given claim 55 has status "IN_REVIEW"
    When I review claim 55 with action "APPROVE" and notes "Late review approved"
    Then the response status is 200
    And the response field "status" is "APPROVED"
```

---

## Feature: Claims List by Policy (Admin)

```gherkin
Feature: Claims List by Policy
  As an Admin
  I want to view all claims for a specific policy
  So that I can identify and manage claims that need review

  Background:
    Given policy with ID 7 has the following claims:
      | claimId | claimType | status    |
      | 42      | MEDICAL   | SUBMITTED |
      | 43      | AUTO      | APPROVED  |

  Scenario: List claims for a policy with multiple claims
    Given I am an authenticated Admin
    When I request claims for policy ID 7
    Then the response status is 200
    And the response is a list of 2 claims
    And the list contains a claim with ID 42 and status "SUBMITTED"
    And the list contains a claim with ID 43 and status "APPROVED"

  Scenario: List claims for a policy with no claims
    Given policy with ID 99 has no claims
    When I request claims for policy ID 99
    Then the response status is 200
    And the response is an empty list

  Scenario: List claims for a non-existent policy
    When I request claims for policy ID 9999
    Then the response status is 200
    And the response is an empty list
```

---

## Feature: Role-Based Access Control (Frontend)

```gherkin
Feature: Role-Based Access Control
  As the System
  I want to restrict page access based on user role
  So that customers cannot access admin features

  Scenario: Unauthenticated user is redirected to login
    Given I am not logged in
    When I navigate to "/submit-claim"
    Then I am redirected to "/login"

  Scenario: Customer cannot access admin pages
    Given I am logged in as a CUSTOMER
    When I navigate to "/admin/policies"
    Then I am redirected to "/"

  Scenario: Admin cannot access customer claim submission
    Given I am logged in as an ADMIN
    When I navigate to "/submit-claim"
    Then I am redirected to "/"

  Scenario: Customer sees correct navigation links
    Given I am logged in as a CUSTOMER
    Then the navbar shows "Submit Claim" and "Track Claim"
    And the navbar does NOT show "Admin Dashboard"

  Scenario: Admin sees correct navigation links
    Given I am logged in as an ADMIN
    Then the navbar shows "Admin Dashboard"
    And the navbar does NOT show "Submit Claim" or "Track Claim"

  Scenario: Session persists on page refresh
    Given I am logged in as a CUSTOMER
    When I refresh the browser
    Then I am still logged in as CUSTOMER
    And I am not redirected to the login page
```
