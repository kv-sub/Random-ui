package com.insurance.claim.service;

import com.insurance.claim.dto.ClaimReviewRequest;
import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.entity.*;
import com.insurance.claim.exception.*;
import com.insurance.claim.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyCoverageRepository policyCoverageRepository;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private Policy activePolicy;
    private PolicyCoverage coverage;
    private ClaimSubmissionRequest validRequest;

    @BeforeEach
    void setUp() {
        activePolicy = Policy.builder()
                .policyId(1L)
                .policyNumber("POL-10001")
                .customerId(1001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .coverageLimit(new BigDecimal("100000.00"))
                .build();

        coverage = PolicyCoverage.builder()
                .coverageId(1L)
                .policy(activePolicy)
                .claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00"))
                .isActive(true)
                .build();

        validRequest = new ClaimSubmissionRequest();
        validRequest.setPolicyNumber("POL-10001");
        validRequest.setClaimType(ClaimType.MEDICAL);
        validRequest.setClaimAmount(new BigDecimal("5000.00"));
        validRequest.setIncidentDate(LocalDate.now().minusDays(3));
        validRequest.setDescription("Medical treatment for accident");
    }

    @Test
    void submitClaim_ValidRequest_ReturnsClaim() {
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));
        when(policyCoverageRepository.findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(1L, ClaimType.MEDICAL))
                .thenReturn(Optional.of(coverage));
        when(claimRepository.findDuplicateClaims(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            c.setClaimId(100L);
            return c;
        });

        var response = claimService.submitClaim(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getClaimId()).isEqualTo(100L);
        assertThat(response.getPolicyNumber()).isEqualTo("POL-10001");
        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    void submitClaim_PolicyNotFound_ThrowsPolicyNotFoundException() {
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("POL-10001");
    }

    @Test
    void submitClaim_InactivePolicy_ThrowsPolicyInactiveException() {
        activePolicy.setStatus(PolicyStatus.INACTIVE);
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(PolicyInactiveException.class);
    }

    @Test
    void submitClaim_InvalidClaimType_ThrowsInvalidClaimTypeException() {
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));
        when(policyCoverageRepository.findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(1L, ClaimType.MEDICAL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(InvalidClaimTypeException.class);
    }

    @Test
    void submitClaim_AmountExceedsLimit_ThrowsCoverageExceededException() {
        validRequest.setClaimAmount(new BigDecimal("99999.00")); // exceeds 50000 limit
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));
        when(policyCoverageRepository.findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(1L, ClaimType.MEDICAL))
                .thenReturn(Optional.of(coverage));

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(CoverageExceededException.class)
                .hasMessageContaining("99999");
    }

    @Test
    void submitClaim_DuplicateClaim_ThrowsDuplicateClaimException() {
        Claim existing = Claim.builder()
                .claimId(50L)
                .policy(activePolicy)
                .claimType(ClaimType.MEDICAL)
                .incidentDate(validRequest.getIncidentDate())
                .build();

        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));
        when(policyCoverageRepository.findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(1L, ClaimType.MEDICAL))
                .thenReturn(Optional.of(coverage));
        when(claimRepository.findDuplicateClaims(any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(DuplicateClaimException.class);
    }

    @Test
    void getClaimStatus_ExistingClaim_ReturnsResponse() {
        Claim claim = Claim.builder()
                .claimId(1L)
                .policy(activePolicy)
                .claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(LocalDate.now().minusDays(3))
                .description("Medical treatment")
                .status(ClaimStatus.SUBMITTED)
                .build();

        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        var response = claimService.getClaimStatus(1L);

        assertThat(response.getClaimId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    void getClaimStatus_NotFound_ThrowsClaimNotFoundException() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.getClaimStatus(999L))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void submitClaim_PolicyExpired_ThrowsPolicyInactiveException() {
        activePolicy.setExpiryDate(LocalDate.now().minusDays(1));
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(PolicyInactiveException.class)
                .hasMessageContaining("OUT_OF_DATE_RANGE");
    }

    @Test
    void submitClaim_PolicyNotYetEffective_ThrowsPolicyInactiveException() {
        activePolicy.setEffectiveDate(LocalDate.now().plusDays(10));
        when(policyRepository.findByPolicyNumberWithCoverages("POL-10001"))
                .thenReturn(Optional.of(activePolicy));

        assertThatThrownBy(() -> claimService.submitClaim(validRequest))
                .isInstanceOf(PolicyInactiveException.class)
                .hasMessageContaining("OUT_OF_DATE_RANGE");
    }

    @Test
    void reviewClaim_ApproveClaim_ReturnsApprovedResponse() {
        Claim claim = buildBasicClaim(1L, ClaimStatus.SUBMITTED);

        ClaimReviewRequest reviewRequest = new ClaimReviewRequest();
        reviewRequest.setAction(ClaimReviewRequest.ReviewAction.APPROVE);
        reviewRequest.setReviewerNotes("Approved after documentation review");

        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = claimService.reviewClaim(1L, reviewRequest);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void reviewClaim_RejectClaim_ReturnsRejectedResponse() {
        Claim claim = buildBasicClaim(1L, ClaimStatus.SUBMITTED);

        ClaimReviewRequest reviewRequest = new ClaimReviewRequest();
        reviewRequest.setAction(ClaimReviewRequest.ReviewAction.REJECT);
        reviewRequest.setReviewerNotes("Insufficient documentation");

        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = claimService.reviewClaim(1L, reviewRequest);

        assertThat(response.getStatus()).isEqualTo(ClaimStatus.REJECTED);
    }

    @Test
    void reviewClaim_ClaimNotFound_ThrowsClaimNotFoundException() {
        ClaimReviewRequest reviewRequest = new ClaimReviewRequest();
        reviewRequest.setAction(ClaimReviewRequest.ReviewAction.APPROVE);
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> claimService.reviewClaim(999L, reviewRequest))
                .isInstanceOf(ClaimNotFoundException.class);
    }

    @Test
    void getClaimsByPolicy_MultipleClaims_ReturnsList() {
        Claim claim1 = buildBasicClaim(1L, ClaimStatus.SUBMITTED);
        Claim claim2 = buildBasicClaim(2L, ClaimStatus.APPROVED);
        when(claimRepository.findByPolicy_PolicyId(1L)).thenReturn(List.of(claim1, claim2));

        var result = claimService.getClaimsByPolicy(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("claimId").containsExactly(1L, 2L);
    }

    @Test
    void getClaimsByPolicy_NoClaims_ReturnsEmptyList() {
        when(claimRepository.findByPolicy_PolicyId(999L)).thenReturn(Collections.emptyList());

        var result = claimService.getClaimsByPolicy(999L);

        assertThat(result).isEmpty();
    }

    private Claim buildBasicClaim(Long id, ClaimStatus status) {
        return Claim.builder()
                .claimId(id)
                .policy(activePolicy)
                .claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(LocalDate.now().minusDays(3))
                .description("Medical treatment")
                .status(status)
                .build();
    }
}
