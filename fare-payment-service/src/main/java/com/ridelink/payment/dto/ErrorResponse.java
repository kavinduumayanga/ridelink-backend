package com.ridelink.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard RideLink error response structure")
public class ErrorResponse {

    @Schema(description = "UTC timestamp when the error occurred", example = "2026-09-27T17:00:00.000+00:00")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error code identifier (VALIDATION_ERROR, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT)", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Human-readable error description", example = "distanceKm must be greater than 0")
    private String message;

    @Schema(description = "Request URI path", example = "/api/fares/estimate")
    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
