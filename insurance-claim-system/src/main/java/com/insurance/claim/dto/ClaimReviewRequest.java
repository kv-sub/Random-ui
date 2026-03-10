package com.insurance.claim.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimReviewRequest {

    public enum ReviewAction {
        APPROVE,
        REJECT
    }

    @NotNull(message = "Review action is required")
    private ReviewAction action;

    private String reviewerNotes;
}
