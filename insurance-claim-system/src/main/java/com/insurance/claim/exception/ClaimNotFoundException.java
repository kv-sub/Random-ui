package com.insurance.claim.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(Long claimId) {
        super("Claim not found with ID: " + claimId);
    }
}
