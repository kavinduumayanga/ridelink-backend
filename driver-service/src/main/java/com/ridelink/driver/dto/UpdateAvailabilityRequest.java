package com.ridelink.driver.dto;

import com.ridelink.driver.domain.DriverAvailability;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update driver availability")
public class UpdateAvailabilityRequest {

    @NotNull(message = "availability is required")
    @Schema(description = "New availability status", example = "AVAILABLE")
    private DriverAvailability availability;

    public UpdateAvailabilityRequest() {
    }

    public UpdateAvailabilityRequest(DriverAvailability availability) {
        this.availability = availability;
    }

    public DriverAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(DriverAvailability availability) {
        this.availability = availability;
    }
}
