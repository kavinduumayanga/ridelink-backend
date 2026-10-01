package com.ridelink.ride.exception;

/**
 * Thrown when communication with Fare & Payment Service fails.
 * Covers connection failures, timeouts, and 5xx responses.
 * Maps to 502 Bad Gateway through GlobalExceptionHandler.
 *
 * Does not expose low-level HTTP client errors or stack traces.
 */
public class FareServiceException extends RuntimeException {

    public FareServiceException(String message) {
        super(message);
    }

    public FareServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
