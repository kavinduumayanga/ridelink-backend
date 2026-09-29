package com.ridelink.ride.exception;

import com.ridelink.ride.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler producing the standard error response
 * defined in API_CONTRACTS.md §1.3.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Jakarta Validation failures → 400 VALIDATION_ERROR.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    /**
     * Handles ride not found → 404 NOT_FOUND.
     */
    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRideNotFound(
            RideNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Handles invalid ride state transitions → 409 CONFLICT.
     * Per API_CONTRACTS.md §1.3 and §5.1.
     */
    @ExceptionHandler(InvalidRideStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRideState(
            InvalidRideStateException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    /**
     * Handles no available drivers found → 404 NOT_FOUND.
     * Per API_CONTRACTS.md §5.5 error codes.
     */
    @ExceptionHandler(NoAvailableDriverException.class)
    public ResponseEntity<ErrorResponse> handleNoAvailableDriver(
            NoAvailableDriverException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    /**
     * Handles Driver Service communication failures → 502 BAD_GATEWAY.
     * Covers connection errors, timeouts, and Driver Service 5xx responses.
     * Does not expose stack traces or low-level HTTP client errors.
     */
    @ExceptionHandler(DriverServiceException.class)
    public ResponseEntity<ErrorResponse> handleDriverServiceException(
            DriverServiceException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "SERVICE_UNAVAILABLE",
                "Driver Service is currently unavailable", request);
    }

    /**
     * Handles Fare Service communication failures → 502 BAD_GATEWAY.
     * Covers connection errors, timeouts, and Fare Service 5xx responses.
     * Does not expose stack traces or low-level HTTP client errors.
     */
    @ExceptionHandler(FareServiceException.class)
    public ResponseEntity<ErrorResponse> handleFareServiceException(
            FareServiceException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "SERVICE_UNAVAILABLE",
                "Fare Service is currently unavailable", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status,
                                                String code,
                                                String message,
                                                HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
