package com.insurance.claim.service;

import com.insurance.claim.dto.PolicyResponse;
import com.insurance.claim.entity.*;
import com.insurance.claim.exception.PolicyNotFoundException;
import com.insurance.claim.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyServiceImpl policyService;

    @Test
    void getPolicy_ValidPolicyNumber_ReturnsPolicyResponse() {
        PolicyCoverage medicalCoverage = PolicyCoverage.builder()
                .coverageId(1L)
                .claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00"))
                .isActive(true)
                .build();

        Policy policy = Policy.builder()
                .policyId(1L)
                .policyNumber("POL-10001")
                .customerId(1001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.of(2025, 1, 1))
                .expiryDate(LocalDate.of(2026, 12, 31))
                .coverageLimit(new BigDecimal("100000.00"))
                .coverages(List.of(medicalCoverage))
                .build();

        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(policy));

        PolicyResponse response = policyService.getPolicy("POL-10001");

        assertThat(response.getPolicyNumber()).isEqualTo("POL-10001");
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(response.getCoverageLimits()).containsKey(ClaimType.MEDICAL);
        assertThat(response.getCoverageLimits().get(ClaimType.MEDICAL))
                .isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    void getPolicy_NotFound_ThrowsPolicyNotFoundException() {
        when(policyRepository.findByPolicyNumberWithCoverages("POL-99999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicy("POL-99999"))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("POL-99999");
    }
}
