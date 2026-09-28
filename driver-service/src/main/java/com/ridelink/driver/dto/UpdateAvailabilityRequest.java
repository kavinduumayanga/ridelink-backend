package com.ridelink.driver.dto;

import com.ridelink.driver.domain.DriverAvailability;
import jakarta.validation.constraints.NotNull;

public class UpdateAvailabilityRequest {

    @NotNull(message = "availability is required")
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
