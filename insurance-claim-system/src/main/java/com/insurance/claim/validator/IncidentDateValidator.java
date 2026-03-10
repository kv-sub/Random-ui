package com.insurance.claim.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class IncidentDateValidator implements ConstraintValidator<ValidIncidentDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull handles null separately
        }
        return !value.isAfter(LocalDate.now());
    }
}
