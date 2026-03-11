package com.insurance.claim.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyNumberValidatorTest {

    private final PolicyNumberValidator validator = new PolicyNumberValidator();

    @Test
    void validPolicyNumbers_ReturnTrue() {
        assertThat(validator.isValid("POL-10001", null)).isTrue();
        assertThat(validator.isValid("POL-ABCDE", null)).isTrue();
        assertThat(validator.isValid("POL-1A2B3", null)).isTrue();
    }

    @Test
    void invalidPolicyNumbers_ReturnFalse() {
        assertThat(validator.isValid("pol-10001", null)).isFalse(); // lowercase
        assertThat(validator.isValid("POL10001", null)).isFalse();  // missing dash
        assertThat(validator.isValid("POL-1234", null)).isFalse();  // too short
        assertThat(validator.isValid("POL-123456", null)).isFalse(); // too long
        assertThat(validator.isValid("POLICY-10001", null)).isFalse();
    }

    @Test
    void nullValue_ReturnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void blankValue_ReturnsTrue() {
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void exactBoundaryLength_FiveChars_ReturnsTrue() {
        assertThat(validator.isValid("POL-00001", null)).isTrue();
        assertThat(validator.isValid("POL-ZZZZZ", null)).isTrue();
    }

    @Test
    void fourCharsAfterPrefix_ReturnsFalse() {
        assertThat(validator.isValid("POL-1234", null)).isFalse();
        assertThat(validator.isValid("POL-ABCD", null)).isFalse();
    }

    @Test
    void sixCharsAfterPrefix_ReturnsFalse() {
        assertThat(validator.isValid("POL-123456", null)).isFalse();
        assertThat(validator.isValid("POL-ABCDEF", null)).isFalse();
    }

    @Test
    void specialCharsAfterPrefix_ReturnsFalse() {
        assertThat(validator.isValid("POL-A@B3C", null)).isFalse();
        assertThat(validator.isValid("POL-A B3C", null)).isFalse();
        assertThat(validator.isValid("POL-A-B3C", null)).isFalse();
    }

    @Test
    void mixedCaseAfterPrefix_ReturnsFalse() {
        assertThat(validator.isValid("POL-aBcDe", null)).isFalse();
        assertThat(validator.isValid("POL-AbCdE", null)).isFalse();
    }

    @Test
    void wrongPrefix_ReturnsFalse() {
        assertThat(validator.isValid("PAL-10001", null)).isFalse();
        assertThat(validator.isValid("POL_10001", null)).isFalse();
        assertThat(validator.isValid("-10001", null)).isFalse();
    }
}
