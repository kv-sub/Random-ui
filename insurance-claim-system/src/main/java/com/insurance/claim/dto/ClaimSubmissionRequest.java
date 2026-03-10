package com.insurance.claim.dto;

import com.insurance.claim.entity.ClaimType;
import com.insurance.claim.validator.ValidIncidentDate;
import com.insurance.claim.validator.ValidPolicyNumber;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClaimSubmissionRequest {

    @NotBlank(message = "Policy number is required")
    @ValidPolicyNumber
    private String policyNumber;

    @NotNull(message = "Claim type is required")
    private ClaimType claimType;

    @NotNull(message = "Claim amount is required")
    @Positive(message = "Claim amount must be positive")
    @DecimalMin(value = "0.01", message = "Claim amount must be at least 0.01")
    private BigDecimal claimAmount;

    @NotNull(message = "Incident date is required")
    @ValidIncidentDate
    private LocalDate incidentDate;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;
}
