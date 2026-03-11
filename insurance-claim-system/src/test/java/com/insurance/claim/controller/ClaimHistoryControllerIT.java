package com.insurance.claim.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ClaimHistoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getClaimHistory_NonExistentClaim_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/claims/999999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Claim Not Found"));
    }

    @Test
    void getClaimHistory_AfterSubmission_HasOneHistoryEntry() throws Exception {
        long claimId = submitClaim("POL-10001", ClaimType.MEDICAL,
                new BigDecimal("5000.00"), LocalDate.now().minusDays(2),
                "Medical claim for history test submission");

        mockMvc.perform(get("/api/v1/claims/" + claimId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].claimId").value(claimId));
    }

    @Test
    void getClaimHistory_AfterReview_HasTwoEntries() throws Exception {
        long claimId = submitClaim("POL-10002", ClaimType.MEDICAL,
                new BigDecimal("3000.00"), LocalDate.now().minusDays(3),
                "Medical claim to be reviewed in history test");

        String reviewJson = "{\"action\":\"APPROVE\",\"reviewerNotes\":\"Claim approved\"}";
        mockMvc.perform(patch("/api/v1/claims/" + claimId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/claims/" + claimId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void getClaimHistory_HistoryContainsReviewerNotes() throws Exception {
        long claimId = submitClaim("POL-10001", ClaimType.DENTAL,
                new BigDecimal("2000.00"), LocalDate.now().minusDays(4),
                "Dental claim for reviewer notes history test");

        String reviewJson = "{\"action\":\"REJECT\",\"reviewerNotes\":\"Incomplete dental records\"}";
        mockMvc.perform(patch("/api/v1/claims/" + claimId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/claims/" + claimId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewerNotes").value("Incomplete dental records"));
    }

    private long submitClaim(String policyNumber, ClaimType claimType,
                              BigDecimal amount, LocalDate incidentDate,
                              String description) throws Exception {
        ClaimSubmissionRequest request = new ClaimSubmissionRequest();
        request.setPolicyNumber(policyNumber);
        request.setClaimType(claimType);
        request.setClaimAmount(amount);
        request.setIncidentDate(incidentDate);
        request.setDescription(description);

        MvcResult result = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("claimId").asLong();
    }
}
