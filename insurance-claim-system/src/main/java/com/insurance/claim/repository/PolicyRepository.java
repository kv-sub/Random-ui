package com.insurance.claim.repository;

import com.insurance.claim.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByCustomerId(Long customerId);

    @Query("SELECT p FROM Policy p LEFT JOIN FETCH p.coverages WHERE p.policyNumber = :policyNumber")
    Optional<Policy> findByPolicyNumberWithCoverages(@Param("policyNumber") String policyNumber);
}
