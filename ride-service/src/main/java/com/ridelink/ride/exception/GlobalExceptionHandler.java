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
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles ride not found → 404 NOT_FOUND.
     */
    @ExceptionHandler(RideNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRideNotFound(
            RideNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles invalid ride state transitions → 409 CONFLICT.
     * Per API_CONTRACTS.md §1.3 and §5.1.
     */
    @ExceptionHandler(InvalidRideStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRideState(
            InvalidRideStateException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles no available drivers found → 404 NOT_FOUND.
     * Per API_CONTRACTS.md §5.5 error codes.
     */
    @ExceptionHandler(NoAvailableDriverException.class)
    public ResponseEntity<ErrorResponse> handleNoAvailableDriver(
            NoAvailableDriverException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles Driver Service communication failures → 502 BAD_GATEWAY.
     * Covers connection errors, timeouts, and Driver Service 5xx responses.
     * Does not expose stack traces or low-level HTTP client errors.
     */
    @ExceptionHandler(DriverServiceException.class)
    public ResponseEntity<ErrorResponse> handleDriverServiceException(
            DriverServiceException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "SERVICE_UNAVAILABLE",
                "Driver Service is currently unavailable",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    /**
     * Handles Fare Service communication failures → 502 BAD_GATEWAY.
     * Covers connection errors, timeouts, and Fare Service 5xx responses.
     * Does not expose stack traces or low-level HTTP client errors.
     */
    @ExceptionHandler(FareServiceException.class)
    public ResponseEntity<ErrorResponse> handleFareServiceException(
            FareServiceException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "SERVICE_UNAVAILABLE",
                "Fare Service is currently unavailable",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }
}


