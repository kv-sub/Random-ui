package com.insurance.claim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.claim.dto.ClaimSubmissionRequest;
import com.insurance.claim.entity.ClaimType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClaimControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitClaim_ValidRequest_Returns201() throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-10001");
        request.setClaimType(ClaimType.MEDICAL);
        request.setClaimAmount(new BigDecimal("5000.00"));
        request.setIncidentDate(LocalDate.now().minusDays(5));
        request.setDescription("Hospital visit following accident");

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").isNumber())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.policyNumber").value("POL-10001"));
    }

    @Test
    void submitClaim_InvalidPolicyNumber_Returns400() throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("INVALID");
        request.setClaimType(ClaimType.MEDICAL);
        request.setClaimAmount(new BigDecimal("5000.00"));
        request.setIncidentDate(LocalDate.now().minusDays(5));
        request.setDescription("Hospital visit following accident");

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void submitClaim_FutureIncidentDate_Returns400() throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-10001");
        request.setClaimType(ClaimType.MEDICAL);
        request.setClaimAmount(new BigDecimal("5000.00"));
        request.setIncidentDate(LocalDate.now().plusDays(1)); // future date
        request.setDescription("Hospital visit following accident");

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitClaim_InactivePolicyNumber_Returns400() throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-10004"); // INACTIVE policy from test data
        request.setClaimType(ClaimType.MEDICAL);
        request.setClaimAmount(new BigDecimal("5000.00"));
        request.setIncidentDate(LocalDate.now().minusDays(5));
        request.setDescription("Hospital visit following accident");

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getClaimStatus_NonExistent_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/claims/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Claim Not Found"));
    }

    @Test
    void getPolicy_ValidPolicyNumber_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-10001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getPolicy_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitClaim_ExceedsCoverageLimit_Returns400() throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-10001");
        request.setClaimType(ClaimType.MEDICAL);
        request.setClaimAmount(new BigDecimal("99000.00")); // exceeds 50000 limit
        request.setIncidentDate(LocalDate.now().minusDays(5));
        request.setDescription("Very expensive hospital stay");

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Coverage Exceeded"));
    }

    @Test
    void getClaimsByPolicy_ExistingPolicy_Returns200() throws Exception {
        // First submit a claim so the policy has claims
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber("POL-10001");
        request.setClaimType(ClaimType.DENTAL);
        request.setClaimAmount(new BigDecimal("1000.00"));
        request.setIncidentDate(LocalDate.now().minusDays(10));
        request.setDescription("Routine dental check and cleaning work");

        var submitResult = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        var body = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        long policyId = body.get("policyId").asLong();

        mockMvc.perform(get("/api/v1/claims/policy/" + policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void reviewClaim_ApproveClaim_Returns200() throws Exception {
        ClaimSubmissionRequest submitRequest = new ClaimSubmissionRequest();
        submitRequest.setPolicyNumber("POL-10002");
        submitRequest.setClaimType(ClaimType.MEDICAL);
        submitRequest.setClaimAmount(new BigDecimal("2000.00"));
        submitRequest.setIncidentDate(LocalDate.now().minusDays(7));
        submitRequest.setDescription("Medical procedure covered under standard plan");

        var submitResult = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var body = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        long claimId = body.get("claimId").asLong();

        String reviewJson = "{\"action\":\"APPROVE\",\"reviewerNotes\":\"Approved after review\"}";
        mockMvc.perform(patch("/api/v1/claims/" + claimId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
