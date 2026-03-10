package com.insurance.claim.service;

import com.insurance.claim.dto.ClaimResponse;
import com.insurance.claim.dto.ClaimReviewRequest;
import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.entity.*;
import com.insurance.claim.exception.*;
import com.insurance.claim.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final PolicyCoverageRepository policyCoverageRepository;

    @Override
    @Transactional
    public ClaimResponse submitClaim(ClaimSubmissionRequest request) {
        log.info("Submitting claim for policy: {}, type: {}", request.getPolicyNumber(), request.getClaimType());

        // 1. Validate and fetch policy
        Policy policy = policyRepository.findByPolicyNumberWithCoverages(request.getPolicyNumber())
                .orElseThrow(() -> new PolicyNotFoundException(request.getPolicyNumber()));

        // 2. Check policy is active
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new PolicyInactiveException(policy.getPolicyNumber(), policy.getStatus().name());
        }

        // 3. Check policy is within valid date range
        var today = java.time.LocalDate.now();
        if (today.isBefore(policy.getEffectiveDate()) || today.isAfter(policy.getExpiryDate())) {
            throw new PolicyInactiveException(policy.getPolicyNumber(), "OUT_OF_DATE_RANGE");
        }

        // 4. Verify coverage for the claim type
        PolicyCoverage coverage = policyCoverageRepository
                .findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(policy.getPolicyId(), request.getClaimType())
                .orElseThrow(() -> new InvalidClaimTypeException(
                        request.getClaimType().name(), request.getPolicyNumber()));

        // 5. Check coverage limit
        if (request.getClaimAmount().compareTo(coverage.getLimitAmount()) > 0) {
            throw new CoverageExceededException(
                    request.getClaimAmount(), coverage.getLimitAmount(), request.getClaimType().name());
        }

        // 6. Duplicate detection — same policy, type, incident_date within 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        List<Claim> duplicates = claimRepository.findDuplicateClaims(
                policy.getPolicyId(), request.getClaimType(), request.getIncidentDate(), oneDayAgo);
        if (!duplicates.isEmpty()) {
            throw new DuplicateClaimException(
                    policy.getPolicyNumber(),
                    request.getClaimType().name(),
                    request.getIncidentDate().toString());
        }

        // 7. Persist claim
        Claim claim = Claim.builder()
                .policy(policy)
                .claimType(request.getClaimType())
                .claimAmount(request.getClaimAmount())
                .incidentDate(request.getIncidentDate())
                .description(request.getDescription())
                .status(ClaimStatus.SUBMITTED)
                .build();

        ClaimHistory historyEntry = ClaimHistory.builder()
                .claim(claim)
                .status(ClaimStatus.SUBMITTED)
                .reviewerNotes("Claim submitted")
                .build();
        claim.getHistory().add(historyEntry);

        Claim saved = claimRepository.save(claim);
        log.info("Claim submitted successfully with ID: {}", saved.getClaimId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getClaimStatus(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
        return mapToResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse reviewClaim(Long claimId, ClaimReviewRequest reviewRequest) {
        log.info("Reviewing claim ID: {}, action: {}", claimId, reviewRequest.getAction());

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));

        ClaimStatus newStatus = reviewRequest.getAction() == ClaimReviewRequest.ReviewAction.APPROVE
                ? ClaimStatus.APPROVED
                : ClaimStatus.REJECTED;

        claim.setStatus(newStatus);

        ClaimHistory historyEntry = ClaimHistory.builder()
                .claim(claim)
                .status(newStatus)
                .reviewerNotes(reviewRequest.getReviewerNotes())
                .build();
        claim.getHistory().add(historyEntry);

        Claim updated = claimRepository.save(claim);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaimsByPolicy(Long policyId) {
        return claimRepository.findByPolicy_PolicyId(policyId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ClaimResponse mapToResponse(Claim claim) {
        return ClaimResponse.builder()
                .claimId(claim.getClaimId())
                .policyId(claim.getPolicy().getPolicyId())
                .policyNumber(claim.getPolicy().getPolicyNumber())
                .claimType(claim.getClaimType())
                .claimAmount(claim.getClaimAmount())
                .incidentDate(claim.getIncidentDate())
                .description(claim.getDescription())
                .status(claim.getStatus())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
