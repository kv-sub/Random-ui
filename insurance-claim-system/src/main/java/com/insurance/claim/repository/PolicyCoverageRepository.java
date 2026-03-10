package com.insurance.claim.repository;

import com.insurance.claim.entity.ClaimType;
import com.insurance.claim.entity.PolicyCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyCoverageRepository extends JpaRepository<PolicyCoverage, Long> {

    Optional<PolicyCoverage> findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue(Long policyId, ClaimType claimType);
}
