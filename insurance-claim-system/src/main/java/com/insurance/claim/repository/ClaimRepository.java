package com.insurance.claim.repository;

import com.insurance.claim.entity.Claim;
import com.insurance.claim.entity.ClaimStatus;
import com.insurance.claim.entity.ClaimType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByPolicy_PolicyId(Long policyId);

    List<Claim> findByStatus(ClaimStatus status);

    /**
     * Detect duplicate claims: same policy, claim type, and incident date within a 24-hour submission window.
     */
    @Query("""
            SELECT c FROM Claim c
            WHERE c.policy.policyId = :policyId
              AND c.claimType = :claimType
              AND c.incidentDate = :incidentDate
              AND c.createdAt >= :since
            """)
    List<Claim> findDuplicateClaims(
            @Param("policyId") Long policyId,
            @Param("claimType") ClaimType claimType,
            @Param("incidentDate") LocalDate incidentDate,
            @Param("since") LocalDateTime since
    );
}
