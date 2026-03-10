package com.insurance.claim.dto;

import com.insurance.claim.entity.ClaimType;
import com.insurance.claim.entity.PolicyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class PolicyResponse {

    private Long policyId;
    private String policyNumber;
    private Long customerId;
    private PolicyStatus status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private BigDecimal coverageLimit;
    private Map<ClaimType, BigDecimal> coverageLimits;
}
