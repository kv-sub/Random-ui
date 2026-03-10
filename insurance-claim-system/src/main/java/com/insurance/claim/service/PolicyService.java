package com.insurance.claim.service;

import com.insurance.claim.dto.PolicyResponse;

public interface PolicyService {

    PolicyResponse getPolicy(String policyNumber);
}
