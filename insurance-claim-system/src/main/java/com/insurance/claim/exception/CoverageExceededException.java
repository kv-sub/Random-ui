package com.insurance.claim.exception;

import java.math.BigDecimal;

public class CoverageExceededException extends RuntimeException {

    public CoverageExceededException(BigDecimal requestedAmount, BigDecimal coverageLimit, String claimType) {
        super(String.format(
                "Claim amount %.2f exceeds coverage limit %.2f for claim type %s",
                requestedAmount, coverageLimit, claimType
        ));
    }
}
