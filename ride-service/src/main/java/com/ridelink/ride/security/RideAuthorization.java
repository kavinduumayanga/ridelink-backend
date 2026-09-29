package com.ridelink.ride.security;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.exception.DriverServiceException;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Component;

@Component("rideAuthorization")
public class RideAuthorization {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;

    public RideAuthorization(RideRepository rideRepository,
                             DriverServiceClient driverServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
    }

    /**
     * Missing rides are allowed through so the service can return the contracted 404.
     * Existing rides are restricted to the passenger account stored on the ride.
     */
    public boolean isPassengerOwner(String rideId, String authenticatedUserId) {
        return rideRepository.findById(rideId)
                .map(ride -> authenticatedUserId.equals(ride.getPassengerId()))
                .orElse(true);
    }

    /**
     * Compares JWT sub (Account userId) with the accountId returned by the
     * contracted Driver Service profile endpoint. It never compares userId
     * directly with the distinct driverId stored on a ride.
     */
    public boolean isAssignedDriver(String rideId, String authenticatedUserId) {
        return rideRepository.findById(rideId)
                .map(ride -> isDriverAccountOwner(ride.getDriverId(), authenticatedUserId))
                .orElse(true);
    }

    private boolean isDriverAccountOwner(String driverId, String authenticatedUserId) {
        if (driverId == null) {
            return false;
        }

        try {
            AvailableDriverResponse driver = driverServiceClient.getDriver(driverId);
            return authenticatedUserId.equals(driver.getAccountId());
        } catch (DriverServiceException ex) {
            // Ownership cannot be established safely, so access is denied.
            return false;
        }
    }
}
