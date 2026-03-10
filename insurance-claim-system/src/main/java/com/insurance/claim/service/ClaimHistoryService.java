package com.insurance.claim.service;

import com.insurance.claim.dto.ClaimHistoryResponse;
import com.insurance.claim.exception.ClaimNotFoundException;
import com.insurance.claim.repository.ClaimHistoryRepository;
import com.insurance.claim.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimHistoryService {

    private final ClaimHistoryRepository claimHistoryRepository;
    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public List<ClaimHistoryResponse> getHistory(Long claimId) {
        // Verify claim exists
        if (!claimRepository.existsById(claimId)) {
            throw new ClaimNotFoundException(claimId);
        }

        return claimHistoryRepository.findByClaimClaimIdOrderByTimestampDesc(claimId).stream()
                .map(h -> ClaimHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .claimId(claimId)
                        .status(h.getStatus())
                        .timestamp(h.getTimestamp())
                        .reviewerNotes(h.getReviewerNotes())
                        .build())
                .toList();
    }
}
