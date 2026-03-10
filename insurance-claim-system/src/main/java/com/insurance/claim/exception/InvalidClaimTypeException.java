package com.insurance.claim.exception;

public class InvalidClaimTypeException extends RuntimeException {

    public InvalidClaimTypeException(String claimType, String policyNumber) {
        super(String.format(
                "Claim type %s is not covered under policy %s or the coverage is inactive",
                claimType, policyNumber
        ));
    }
}
