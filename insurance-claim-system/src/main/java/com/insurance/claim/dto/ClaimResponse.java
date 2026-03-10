package com.insurance.claim.dto;

import com.insurance.claim.entity.ClaimStatus;
import com.insurance.claim.entity.ClaimType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ClaimResponse {

    private Long claimId;
    private Long policyId;
    private String policyNumber;
    private ClaimType claimType;
    private BigDecimal claimAmount;
    private LocalDate incidentDate;
    private String description;
    private ClaimStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
