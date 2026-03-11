package com.insurance.claim.repository;

import com.insurance.claim.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PolicyRepositoryTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private PolicyCoverageRepository policyCoverageRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void findByPolicyNumber_WhenExists_ReturnsPolicy() {
        Policy saved = policyRepository.save(Policy.builder()
                .policyNumber("POL-TSTX1")
                .customerId(2001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(3))
                .expiryDate(LocalDate.now().plusMonths(9))
                .coverageLimit(new BigDecimal("75000.00"))
                .build());

        Optional<Policy> found = policyRepository.findByPolicyNumber("POL-TSTX1");

        assertThat(found).isPresent();
        assertThat(found.get().getPolicyId()).isEqualTo(saved.getPolicyId());
        assertThat(found.get().getCustomerId()).isEqualTo(2001L);
    }

    @Test
    void findByPolicyNumber_WhenNotExists_ReturnsEmpty() {
        Optional<Policy> found = policyRepository.findByPolicyNumber("POL-NOXXX");

        assertThat(found).isEmpty();
    }

    @Test
    void findByCustomerId_ReturnsPoliciesForCustomer() {
        policyRepository.save(Policy.builder()
                .policyNumber("POL-CUS01")
                .customerId(3001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(3))
                .expiryDate(LocalDate.now().plusMonths(9))
                .coverageLimit(new BigDecimal("75000.00"))
                .build());

        policyRepository.save(Policy.builder()
                .policyNumber("POL-CUS02")
                .customerId(3001L)
                .status(PolicyStatus.INACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(18))
                .expiryDate(LocalDate.now().minusDays(1))
                .coverageLimit(new BigDecimal("50000.00"))
                .build());

        policyRepository.save(Policy.builder()
                .policyNumber("POL-OTRX1")
                .customerId(9999L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(1))
                .expiryDate(LocalDate.now().plusMonths(11))
                .coverageLimit(new BigDecimal("60000.00"))
                .build());

        List<Policy> policies = policyRepository.findByCustomerId(3001L);

        assertThat(policies).hasSize(2);
        assertThat(policies).extracting("customerId").allMatch(id -> id.equals(3001L));
    }

    @Test
    void findByCustomerId_WhenNoPolicies_ReturnsEmpty() {
        List<Policy> policies = policyRepository.findByCustomerId(8888L);

        assertThat(policies).isEmpty();
    }

    @Test
    void findByPolicyNumberWithCoverages_ReturnsPolicyWithCoverages() {
        Policy policy = policyRepository.save(Policy.builder()
                .policyNumber("POL-COVXT")
                .customerId(4001L)
                .status(PolicyStatus.ACTIVE)
                .effectiveDate(LocalDate.now().minusMonths(3))
                .expiryDate(LocalDate.now().plusMonths(9))
                .coverageLimit(new BigDecimal("100000.00"))
                .build());

        policyCoverageRepository.save(PolicyCoverage.builder()
                .policy(policy)
                .claimType(ClaimType.MEDICAL)
                .limitAmount(new BigDecimal("50000.00"))
                .isActive(true)
                .build());

        policyCoverageRepository.save(PolicyCoverage.builder()
                .policy(policy)
                .claimType(ClaimType.DENTAL)
                .limitAmount(new BigDecimal("10000.00"))
                .isActive(true)
                .build());

        // Flush and clear to force reload from DB, bypassing first-level cache
        em.flush();
        em.clear();

        Optional<Policy> found = policyRepository.findByPolicyNumberWithCoverages("POL-COVXT");

        assertThat(found).isPresent();
        assertThat(found.get().getCoverages()).hasSize(2);
    }

    @Test
    void findByPolicyNumberWithCoverages_WhenNotExists_ReturnsEmpty() {
        Optional<Policy> found = policyRepository.findByPolicyNumberWithCoverages("POL-YYYYY");

        assertThat(found).isEmpty();
    }

    @Test
    void save_PersistsPolicyWithGeneratedId() {
        Policy policy = policyRepository.save(Policy.builder()
                .policyNumber("POL-SVX00")
                .customerId(5001L)
                .status(PolicyStatus.PENDING)
                .effectiveDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .coverageLimit(new BigDecimal("40000.00"))
                .build());

        assertThat(policy.getPolicyId()).isNotNull();
        assertThat(policy.getPolicyId()).isPositive();
    }
}
