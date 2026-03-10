package com.insurance.claim.service;

import com.insurance.claim.dto.ClaimResponse;
import com.insurance.claim.dto.ClaimReviewRequest;
import com.insurance.claim.dto.ClaimSubmissionRequest;

import java.util.List;

public interface ClaimService {

    ClaimResponse submitClaim(ClaimSubmissionRequest request);

    ClaimResponse getClaimStatus(Long claimId);

    ClaimResponse reviewClaim(Long claimId, ClaimReviewRequest reviewRequest);

    List<ClaimResponse> getClaimsByPolicy(Long policyId);
}
