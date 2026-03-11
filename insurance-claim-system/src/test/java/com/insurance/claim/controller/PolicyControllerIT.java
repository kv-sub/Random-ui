package com.insurance.claim.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PolicyControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPolicy_ActivePolicy_ReturnsFullDetails() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-10001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.coverageLimits").isMap())
                .andExpect(jsonPath("$.policyId").isNumber())
                .andExpect(jsonPath("$.customerId").isNumber());
    }

    @Test
    void getPolicy_SecondActivePolicy_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-10002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-10002"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getPolicy_InactivePolicy_Returns200WithInactiveStatus() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-10004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyNumber").value("POL-10004"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void getPolicy_NonExistentPolicy_Returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Policy Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getPolicy_PolicyWithCoverages_ReturnsCoverageLimitsMap() throws Exception {
        mockMvc.perform(get("/api/v1/policies/POL-10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageLimits.MEDICAL").isNumber())
                .andExpect(jsonPath("$.coverageLimits.DENTAL").isNumber());
    }
}
