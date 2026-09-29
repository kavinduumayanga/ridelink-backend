package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for ride creation, retrieval, and driver assignment.
 *
 * Responsibilities:
 * - Create rides with status REQUESTED, driverId null, finalFare null.
 * - Retrieve a single ride by rideId (404 if not found).
 * - Retrieve passenger ride history, newest first.
 * - Assign a driver to a REQUESTED ride by calling Driver Service.
 * - Map between Ride document and RideResponse DTO.
 *
 * estimatedFare is left null for now — Fare Service integration will
 * populate it in a later task (API_CONTRACTS.md §6.2).
 */
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;

    public RideService(RideRepository rideRepository,
                       DriverServiceClient driverServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
    }

    /**
     * Creates a new ride in REQUESTED status.
     * driverId, estimatedFare, and finalFare are intentionally null.
     * estimatedFare will be populated by Fare Service integration later.
     */
    public RideResponse createRide(CreateRideRequest request) {
        Instant now = Instant.now();

        Ride ride = new Ride();
        ride.setPassengerId(request.getPassengerId());
        ride.setDriverId(null);
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropoffLocation(request.getDropoffLocation());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setDropoffLatitude(request.getDropoffLatitude());
        ride.setDropoffLongitude(request.getDropoffLongitude());
        ride.setDistanceKm(request.getDistanceKm());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(null);
        ride.setFinalFare(null);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);

        Ride saved = rideRepository.save(ride);
        return toRideResponse(saved);
    }

    /**
     * Retrieves a ride by its rideId.
     * @throws RideNotFoundException if ride does not exist (→ 404).
     */
    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        return toRideResponse(ride);
    }

    /**
     * Retrieves all rides for a passenger, newest first.
     * Returns an empty list if no rides exist (never 404).
     */
    public List<RideResponse> getRidesByPassengerId(String passengerId) {
        List<Ride> rides = rideRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId);
        return rides.stream()
                .map(this::toRideResponse)
                .toList();
    }

    /**
     * Assigns a driver to a ride.
     *
     * Flow (API_CONTRACTS.md §5.5 and §6.1):
     * 1. Load ride by rideId (404 if not found).
     * 2. Validate ride is in REQUESTED status (409 if not).
     * 3. Call Driver Service GET /api/drivers/available?serviceArea={pickupLocation}.
     * 4. Select the FIRST eligible driver returned (deterministic, simple rule).
     * 5. Store only driverId in the ride.
     * 6. Transition status: REQUESTED → ASSIGNED.
     * 7. Update updatedAt timestamp.
     * 8. Persist and return RideResponse.
     *
     * @param rideId the ride to assign a driver to
     * @return RideResponse with status ASSIGNED and driverId populated
     * @throws RideNotFoundException if ride does not exist
     * @throws InvalidRideStateException if ride is not in REQUESTED status
     * @throws NoAvailableDriverException if no available drivers are found
     * @throws com.ridelink.ride.exception.DriverServiceException if Driver Service is unreachable
     */
    public RideResponse assignDriver(String rideId) {
        // Step 1: Load ride
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));

        // Step 2: Validate ride is REQUESTED
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException(
                    "Cannot assign driver: ride status is " + ride.getStatus()
                            + ", expected REQUESTED");
        }

        // Step 3: Call Driver Service for available drivers
        // Use pickupLocation as the serviceArea query parameter
        String serviceArea = ride.getPickupLocation();
        List<AvailableDriverResponse> availableDrivers =
                driverServiceClient.getAvailableDrivers(serviceArea);

        // Step 4: Check for available drivers
        if (availableDrivers.isEmpty()) {
            throw new NoAvailableDriverException(serviceArea);
        }

        // Step 5: Select FIRST eligible driver (deterministic, simple rule)
        AvailableDriverResponse selectedDriver = availableDrivers.get(0);

        // Step 6: Store only driverId in ride
        ride.setDriverId(selectedDriver.getDriverId());

        // Step 7: Transition status REQUESTED → ASSIGNED
        ride.setStatus(RideStatus.ASSIGNED);

        // Step 8: Update timestamp
        ride.setUpdatedAt(Instant.now());

        // Step 9: Persist
        Ride saved = rideRepository.save(ride);

        // Step 10: Return RideResponse
        return toRideResponse(saved);
    }

    /**
     * Maps a Ride document to a RideResponse DTO.
     * Document `id` becomes `rideId` in the response.
     */
    private RideResponse toRideResponse(Ride ride) {
        RideResponse response = new RideResponse();
        response.setRideId(ride.getId());
        response.setPassengerId(ride.getPassengerId());
        response.setDriverId(ride.getDriverId());
        response.setPickupLocation(ride.getPickupLocation());
        response.setDropoffLocation(ride.getDropoffLocation());
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setDropoffLatitude(ride.getDropoffLatitude());
        response.setDropoffLongitude(ride.getDropoffLongitude());
        response.setDistanceKm(ride.getDistanceKm());
        response.setStatus(ride.getStatus().name());
        response.setEstimatedFare(ride.getEstimatedFare());
        response.setFinalFare(ride.getFinalFare());
        response.setCreatedAt(ride.getCreatedAt());
        response.setUpdatedAt(ride.getUpdatedAt());
        return response;
    }
}

