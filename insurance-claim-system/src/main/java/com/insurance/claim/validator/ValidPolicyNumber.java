package com.insurance.claim.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PolicyNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPolicyNumber {

    String message() default "Policy number must match format POL-XXXXX (e.g., POL-12345)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
