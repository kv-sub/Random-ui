package com.insurance.claim.repository;

import com.insurance.claim.entity.ClaimHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, Long> {

    List<ClaimHistory> findByClaimClaimIdOrderByTimestampDesc(Long claimId);
}
