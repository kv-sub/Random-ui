package com.insurance.claim.repository;

import com.insurance.claim.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ClaimRepositoryTest {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyCoverageRepository policyCoverageRepository;

    private Policy savedPolicy;

    @BeforeEach
    void setUp() {
        savedPolicy = policyRepository.save(Policy.builder()
                .policyNumber("POL-REPOT")
                .customerId(9001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .coverageLimit(new BigDecimal("100000.00"))
                .build());

        policyCoverageRepository.save(PolicyCoverage.builder()
                .policy(savedPolicy)
                .claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00"))
                .isActive(true)
                .build());
    }

    @Test
    void findByPolicy_PolicyId_ReturnsMatchingClaims() {
        Claim claim = claimRepository.save(Claim.builder()
                .policy(savedPolicy)
                .claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(LocalDate.now().minusDays(5))
                .description("Test medical claim for policy filter")
                .status(ClaimStatus.SUBMITTED)
                .build());

        List<Claim> found = claimRepository.findByPolicy_PolicyId(savedPolicy.getPolicyId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getClaimId()).isEqualTo(claim.getClaimId());
    }

    @Test
    void findByPolicy_PolicyId_WhenNoClaimsForPolicy_ReturnsEmpty() {
        List<Claim> found = claimRepository.findByPolicy_PolicyId(savedPolicy.getPolicyId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByStatus_ReturnsMatchingClaims() {
        claimRepository.save(Claim.builder()
                .policy(savedPolicy).claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("1000.00"))
                .incidentDate(LocalDate.now().minusDays(1))
                .description("Submitted claim for status filter test")
                .status(ClaimStatus.SUBMITTED).build());

        Claim approvedClaim = claimRepository.save(Claim.builder()
                .policy(savedPolicy).claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("2000.00"))
                .incidentDate(LocalDate.now().minusDays(2))
                .description("Approved claim for status filter test")
                .status(ClaimStatus.APPROVED).build());

        List<Claim> submitted = claimRepository.findByStatus(ClaimStatus.SUBMITTED);
        List<Claim> approved = claimRepository.findByStatus(ClaimStatus.APPROVED);

        assertThat(submitted).hasSize(1);
        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).getClaimId()).isEqualTo(approvedClaim.getClaimId());
    }

    @Test
    void findByStatus_WhenNoClaimsWithStatus_ReturnsEmpty() {
        List<Claim> rejected = claimRepository.findByStatus(ClaimStatus.REJECTED);

        assertThat(rejected).isEmpty();
    }

    @Test
    void findDuplicateClaims_WhenDuplicateExists_ReturnsIt() {
        LocalDate incidentDate = LocalDate.now().minusDays(1);
        Claim existing = claimRepository.save(Claim.builder()
                .policy(savedPolicy).claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(incidentDate)
                .description("Original claim for duplicate detection")
                .status(ClaimStatus.SUBMITTED).build());

        List<Claim> duplicates = claimRepository.findDuplicateClaims(
                savedPolicy.getPolicyId(),
                ClaimType.MEDICAL,
                incidentDate,
                LocalDateTime.now().minusHours(24)
        );

        assertThat(duplicates).hasSize(1);
        assertThat(duplicates.get(0).getClaimId()).isEqualTo(existing.getClaimId());
    }

    @Test
    void findDuplicateClaims_WhenNoDuplicate_ReturnsEmptyList() {
        LocalDate incidentDate = LocalDate.now().minusDays(5);

        List<Claim> duplicates = claimRepository.findDuplicateClaims(
                savedPolicy.getPolicyId(),
                ClaimType.MEDICAL,
                incidentDate,
                LocalDateTime.now().minusHours(24)
        );

        assertThat(duplicates).isEmpty();
    }

    @Test
    void findDuplicateClaims_WhenCutoffIsAfterCreation_ReturnsEmpty() {
        LocalDate incidentDate = LocalDate.now().minusDays(1);
        claimRepository.save(Claim.builder()
                .policy(savedPolicy).claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(incidentDate)
                .description("Claim for cutoff test")
                .status(ClaimStatus.SUBMITTED).build());

        // Use a future cutoff — claim was created before this, so createdAt < cutoff → no match
        List<Claim> duplicates = claimRepository.findDuplicateClaims(
                savedPolicy.getPolicyId(),
                ClaimType.MEDICAL,
                incidentDate,
                LocalDateTime.now().plusHours(1)
        );

        assertThat(duplicates).isEmpty();
    }

    @Test
    void findDuplicateClaims_DifferentClaimType_ReturnsEmpty() {
        LocalDate incidentDate = LocalDate.now().minusDays(1);
        claimRepository.save(Claim.builder()
                .policy(savedPolicy).claimType(ClaimType.MEDICAL)
                .claimAmount(new BigDecimal("5000.00"))
                .incidentDate(incidentDate)
                .description("Medical claim for type filter test")
                .status(ClaimStatus.SUBMITTED).build());

        List<Claim> duplicates = claimRepository.findDuplicateClaims(
                savedPolicy.getPolicyId(),
                ClaimType.DENTAL,
                incidentDate,
                LocalDateTime.now().minusHours(24)
        );

        assertThat(duplicates).isEmpty();
    }
}
