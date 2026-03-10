package com.insurance.claim.controller;

import com.insurance.claim.dto.ClaimResponse;
import com.insurance.claim.dto.ClaimReviewRequest;
import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Insurance claim management endpoints")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @Operation(summary = "Submit a new insurance claim")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Claim submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or coverage exceeded"),
            @ApiResponse(responseCode = "404", description = "Policy not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate claim detected")
    })
    public ResponseEntity<ClaimResponse> submitClaim(
            @Valid @RequestBody ClaimSubmissionRequest request) {
        ClaimResponse response = claimService.submitClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{claimId}")
    @Operation(summary = "Get claim status by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim found"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<ClaimResponse> getClaimStatus(
            @Parameter(description = "Claim ID") @PathVariable Long claimId) {
        return ResponseEntity.ok(claimService.getClaimStatus(claimId));
    }

    @PatchMapping("/{claimId}/review")
    @Operation(summary = "Approve or reject a claim (reviewer endpoint)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim reviewed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid review request"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<ClaimResponse> reviewClaim(
            @Parameter(description = "Claim ID") @PathVariable Long claimId,
            @Valid @RequestBody ClaimReviewRequest reviewRequest) {
        return ResponseEntity.ok(claimService.reviewClaim(claimId, reviewRequest));
    }

    @GetMapping("/policy/{policyId}")
    @Operation(summary = "Get all claims for a policy")
    @ApiResponse(responseCode = "200", description = "Claims retrieved")
    public ResponseEntity<List<ClaimResponse>> getClaimsByPolicy(
            @Parameter(description = "Policy ID") @PathVariable Long policyId) {
        return ResponseEntity.ok(claimService.getClaimsByPolicy(policyId));
    }
}
