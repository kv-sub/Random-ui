package com.insurance.claim.exception;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyNumber) {
        super("Policy not found: " + policyNumber);
    }

    public PolicyNotFoundException(Long policyId) {
        super("Policy not found with ID: " + policyId);
    }
}
