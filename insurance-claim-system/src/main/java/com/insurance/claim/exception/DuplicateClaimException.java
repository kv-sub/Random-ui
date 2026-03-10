package com.insurance.claim.exception;

public class DuplicateClaimException extends RuntimeException {

    public DuplicateClaimException(String policyNumber, String claimType, String incidentDate) {
        super(String.format(
                "Duplicate claim detected: policy=%s, type=%s, incident_date=%s. A similar claim was submitted within the last 24 hours.",
                policyNumber, claimType, incidentDate
        ));
    }
}
