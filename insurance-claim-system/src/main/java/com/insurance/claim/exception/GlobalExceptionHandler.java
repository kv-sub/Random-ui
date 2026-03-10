package com.insurance.claim.exception;

import com.insurance.claim.util.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        log.warn("Validation failed: {}", details);

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePolicyNotFound(PolicyNotFoundException ex) {
        log.warn("Policy not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Policy Not Found", ex.getMessage());
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClaimNotFound(ClaimNotFoundException ex) {
        log.warn("Claim not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Claim Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateClaimException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateClaim(DuplicateClaimException ex) {
        log.warn("Duplicate claim: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Duplicate Claim", ex.getMessage());
    }

    @ExceptionHandler(CoverageExceededException.class)
    public ResponseEntity<ErrorResponse> handleCoverageExceeded(CoverageExceededException ex) {
        log.warn("Coverage exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Coverage Exceeded", ex.getMessage());
    }

    @ExceptionHandler(InvalidClaimTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClaimType(InvalidClaimTypeException ex) {
        log.warn("Invalid claim type: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Claim Type", ex.getMessage());
    }

    @ExceptionHandler(PolicyInactiveException.class)
    public ResponseEntity<ErrorResponse> handlePolicyInactive(PolicyInactiveException ex) {
        log.warn("Policy inactive: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Policy Inactive", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("No resource found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
