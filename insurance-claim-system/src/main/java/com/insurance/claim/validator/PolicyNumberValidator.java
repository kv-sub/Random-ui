package com.insurance.claim.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PolicyNumberValidator implements ConstraintValidator<ValidPolicyNumber, String> {

    // Accepts: POL- followed by 5 alphanumeric characters
    private static final String POLICY_NUMBER_REGEX = "^POL-[A-Z0-9]{5}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // @NotBlank handles null/blank separately
        }
        return value.matches(POLICY_NUMBER_REGEX);
    }
}
