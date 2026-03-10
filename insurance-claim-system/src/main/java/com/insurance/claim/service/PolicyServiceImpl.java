package com.insurance.claim.service;

import com.insurance.claim.dto.PolicyResponse;
import com.insurance.claim.entity.Policy;
import com.insurance.claim.exception.PolicyNotFoundException;
import com.insurance.claim.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(String policyNumber) {
        log.info("Fetching policy: {}", policyNumber);

        Policy policy = policyRepository.findByPolicyNumberWithCoverages(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));

        return mapToResponse(policy);
    }

    private PolicyResponse mapToResponse(Policy policy) {
        var coverageLimits = policy.getCoverages().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toMap(
                        c -> c.getClaimType(),
                        c -> c.getLimitAmount()
                ));

        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getCustomerId())
                .status(policy.getStatus())
                .effectiveDate(policy.getEffectiveDate())
                .expiryDate(policy.getExpiryDate())
                .coverageLimit(policy.getCoverageLimit())
                .coverageLimits(coverageLimits)
                .build();
    }
}
