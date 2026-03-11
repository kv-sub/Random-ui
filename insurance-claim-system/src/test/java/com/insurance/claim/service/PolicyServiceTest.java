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
import java.util.Collections;
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

    @Test
    void getPolicy_WithMultipleCoverages_ReturnsAllActiveLimits() {
        PolicyCoverage medCoverage = PolicyCoverage.builder()
                .coverageId(1L).claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00")).isActive(true).build();
        PolicyCoverage dentalCoverage = PolicyCoverage.builder()
                .coverageId(2L).claimType(ClaimType.DENTAL)
                .limitAmount(new BigDecimal("10000.00")).isActive(true).build();

        Policy policy = Policy.builder()
                .policyId(1L).policyNumber("POL-10001").customerId(1001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .coverageLimit(new BigDecimal("100000.00"))
                .coverages(List.of(medCoverage, dentalCoverage))
                .build();

        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(policy));

        PolicyResponse response = policyService.getPolicy("POL-10001");

        assertThat(response.getCoverageLimits()).hasSize(2);
        assertThat(response.getCoverageLimits()).containsKey(ClaimType.MEDICAL);
        assertThat(response.getCoverageLimits()).containsKey(ClaimType.DENTAL);
    }

    @Test
    void getPolicy_WithInactiveCoverage_ExcludesInactiveFromMap() {
        PolicyCoverage activeCoverage = PolicyCoverage.builder()
                .coverageId(1L).claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00")).isActive(true).build();
        PolicyCoverage inactiveCoverage = PolicyCoverage.builder()
                .coverageId(2L).claimType(ClaimType.DENTAL)
                .limitAmount(new BigDecimal("10000.00")).isActive(false).build();

        Policy policy = Policy.builder()
                .policyId(1L).policyNumber("POL-10001").customerId(1001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .coverageLimit(new BigDecimal("100000.00"))
                .coverages(List.of(activeCoverage, inactiveCoverage))
                .build();

        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(policy));

        PolicyResponse response = policyService.getPolicy("POL-10001");

        assertThat(response.getCoverageLimits()).hasSize(1);
        assertThat(response.getCoverageLimits()).containsKey(ClaimType.MEDICAL);
        assertThat(response.getCoverageLimits()).doesNotContainKey(ClaimType.DENTAL);
    }

    @Test
    void getPolicy_WithNoCoverages_ReturnsEmptyMap() {
        Policy policy = Policy.builder()
                .policyId(2L).policyNumber("POL-10002").customerId(1002L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(1))
                .expiryDate(LocalDate.now().plusMonths(12))
                .coverageLimit(new BigDecimal("50000.00"))
                .coverages(Collections.emptyList())
                .build();

        when(policyRepository.findByPolicyNumberWithCoverages("POL-10002"))
                .thenReturn(Optional.of(policy));

        PolicyResponse response = policyService.getPolicy("POL-10002");

        assertThat(response.getCoverageLimits()).isEmpty();
        assertThat(response.getPolicyId()).isEqualTo(2L);
        assertThat(response.getCustomerId()).isEqualTo(1002L);
    }
}
