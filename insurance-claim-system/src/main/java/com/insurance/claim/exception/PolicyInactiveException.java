package com.insurance.claim.exception;

public class PolicyInactiveException extends RuntimeException {

    public PolicyInactiveException(String policyNumber, String status) {
        super(String.format("Policy %s is not active. Current status: %s", policyNumber, status));
    }
}
