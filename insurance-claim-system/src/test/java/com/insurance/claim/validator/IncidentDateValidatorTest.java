package com.insurance.claim.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentDateValidatorTest {

    private IncidentDateValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IncidentDateValidator();
    }

    @Test
    void nullDate_ReturnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void today_ReturnsTrue() {
        assertThat(validator.isValid(LocalDate.now(), null)).isTrue();
    }

    @Test
    void yesterday_ReturnsTrue() {
        assertThat(validator.isValid(LocalDate.now().minusDays(1), null)).isTrue();
    }

    @Test
    void pastDateOneYear_ReturnsTrue() {
        assertThat(validator.isValid(LocalDate.now().minusYears(1), null)).isTrue();
    }

    @Test
    void arbitraryHistoricalDate_ReturnsTrue() {
        assertThat(validator.isValid(LocalDate.of(2020, 1, 15), null)).isTrue();
    }

    @Test
    void tomorrow_ReturnsFalse() {
        assertThat(validator.isValid(LocalDate.now().plusDays(1), null)).isFalse();
    }

    @Test
    void nextWeek_ReturnsFalse() {
        assertThat(validator.isValid(LocalDate.now().plusDays(7), null)).isFalse();
    }

    @Test
    void farFutureDate_ReturnsFalse() {
        assertThat(validator.isValid(LocalDate.now().plusYears(1), null)).isFalse();
    }
}
