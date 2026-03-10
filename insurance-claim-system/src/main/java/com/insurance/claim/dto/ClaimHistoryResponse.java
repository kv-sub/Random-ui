package com.insurance.claim.dto;

import com.insurance.claim.entity.ClaimStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClaimHistoryResponse {

    private Long historyId;
    private Long claimId;
    private ClaimStatus status;
    private LocalDateTime timestamp;
    private String reviewerNotes;
}
