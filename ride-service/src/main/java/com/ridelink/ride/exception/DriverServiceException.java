package com.ridelink.ride.exception;

/**
 * Thrown when communication with Driver Service fails.
 * Covers connection failures, timeouts, and 5xx responses.
 * Maps to 502 Bad Gateway through GlobalExceptionHandler.
 *
 * Does not expose low-level HTTP client errors or stack traces.
 */
public class DriverServiceException extends RuntimeException {

    public DriverServiceException(String message) {
        super(message);
    }

    public DriverServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
