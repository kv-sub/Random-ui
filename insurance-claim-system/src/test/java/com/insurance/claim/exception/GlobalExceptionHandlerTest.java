package com.insurance.claim.exception;

import com.insurance.claim.util.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // Dummy method used to construct MethodParameter (required by MethodArgumentNotValidException)
    @SuppressWarnings("unused")
    private static void dummyValidatedMethod(Object payload) {}

    @Test
    void handlePolicyNotFound_Returns404WithCorrectError() {
        PolicyNotFoundException ex = new PolicyNotFoundException("POL-X");
        ResponseEntity<ErrorResponse> response = handler.handlePolicyNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Policy Not Found");
        assertThat(response.getBody().getMessage()).contains("POL-X");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleClaimNotFound_Returns404WithCorrectError() {
        ClaimNotFoundException ex = new ClaimNotFoundException(42L);
        ResponseEntity<ErrorResponse> response = handler.handleClaimNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Claim Not Found");
        assertThat(response.getBody().getMessage()).contains("42");
    }

    @Test
    void handleDuplicateClaim_Returns409WithCorrectError() {
        DuplicateClaimException ex = new DuplicateClaimException("POL-10001", "MEDICAL", "2026-01-01");
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateClaim(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Duplicate Claim");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void handleCoverageExceeded_Returns400WithCorrectError() {
        CoverageExceededException ex = new CoverageExceededException(
                new BigDecimal("99000.00"),
                new BigDecimal("50000.00"),
                "MEDICAL"
        );
        ResponseEntity<ErrorResponse> response = handler.handleCoverageExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Coverage Exceeded");
        assertThat(response.getBody().getMessage()).contains("99000");
    }

    @Test
    void handleInvalidClaimType_Returns400WithCorrectError() {
        InvalidClaimTypeException ex = new InvalidClaimTypeException("VISION", "POL-10001");
        ResponseEntity<ErrorResponse> response = handler.handleInvalidClaimType(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Invalid Claim Type");
        assertThat(response.getBody().getMessage()).contains("VISION");
    }

    @Test
    void handlePolicyInactive_Returns400WithCorrectError() {
        PolicyInactiveException ex = new PolicyInactiveException("POL-10004", "INACTIVE");
        ResponseEntity<ErrorResponse> response = handler.handlePolicyInactive(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Policy Inactive");
        assertThat(response.getBody().getMessage()).contains("POL-10004");
    }

    @Test
    void handleNoResourceFound_Returns404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path");
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
    }

    @Test
    void handleUnexpected_Returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Something went wrong unexpectedly");
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }

    @Test
    void handleValidationErrors_Returns400WithFieldErrorDetails() throws Exception {
        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyValidatedMethod", Object.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "policyNumber", "Policy number is required"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getDetails()).contains("Policy number is required");
    }

    @Test
    void handleValidationErrors_MultipleFieldErrors_IncludesAllMessages() throws Exception {
        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyValidatedMethod", Object.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "policyNumber", "Policy number is required"));
        bindingResult.addError(new FieldError("request", "claimAmount", "Claim amount must be positive"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails()).hasSize(2);
        assertThat(response.getBody().getDetails()).contains(
                "Policy number is required",
                "Claim amount must be positive"
        );
    }
}
