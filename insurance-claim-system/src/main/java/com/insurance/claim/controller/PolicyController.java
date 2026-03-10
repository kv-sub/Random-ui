package com.insurance.claim.controller;

import com.insurance.claim.dto.PolicyResponse;
import com.insurance.claim.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Insurance policy lookup endpoints")
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/{policyNumber}")
    @Operation(summary = "Get policy details by policy number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Policy found"),
            @ApiResponse(responseCode = "404", description = "Policy not found")
    })
    public ResponseEntity<PolicyResponse> getPolicy(
            @Parameter(description = "Policy number (e.g., POL-12345)") @PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.getPolicy(policyNumber));
    }
}
