package com.insurance.claim.controller;

import com.insurance.claim.dto.ClaimHistoryResponse;
import com.insurance.claim.service.ClaimHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claim History", description = "Claim status history endpoints")
public class ClaimHistoryController {

    private final ClaimHistoryService claimHistoryService;

    @GetMapping("/{claimId}/history")
    @Operation(summary = "Get claim status history")
    @ApiResponse(responseCode = "200", description = "History retrieved")
    @ApiResponse(responseCode = "404", description = "Claim not found")
    public ResponseEntity<List<ClaimHistoryResponse>> getClaimHistory(
            @Parameter(description = "Claim ID") @PathVariable Long claimId) {
        return ResponseEntity.ok(claimHistoryService.getHistory(claimId));
    }
}
