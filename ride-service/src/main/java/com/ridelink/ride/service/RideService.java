package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareRequest;
import com.ridelink.ride.dto.FareResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Business logic for ride creation, retrieval, driver assignment,
 * and ride completion with fare integration.
 *
 * Responsibilities:
 * - Create rides with status REQUESTED, calling Fare Service for estimated
 * fare.
 * - Retrieve a single ride by rideId (404 if not found).
 * - Retrieve passenger ride history, newest first.
 * - Assign a driver to a REQUESTED ride by calling Driver Service.
 * - Complete a ride (IN_PROGRESS → COMPLETED) by calling Fare Service for final
 * fare.
 * - Map between Ride document and RideResponse DTO.
 *
 * Fare calculation is owned by Fare & Payment Service (API_CONTRACTS.md §6.2,
 * §6.3).
 * Ride Service never calculates fares locally.
 */
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;
    private final FareServiceClient fareServiceClient;

    public RideService(RideRepository rideRepository,
            DriverServiceClient driverServiceClient,
            FareServiceClient fareServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
        this.fareServiceClient = fareServiceClient;
    }

    /**
     * Creates a new ride in REQUESTED status.
     *
     * Flow (API_CONTRACTS.md §5.2 and §6.2):
     * 1. Call Fare Service POST /api/fares/estimate with a temporary rideId and
     * distanceKm.
     * 2. Store the returned totalFare as estimatedFare.
     * 3. Persist the ride with status REQUESTED.
     *
     * If Fare Service fails, the ride is NOT persisted — a FareServiceException
     * propagates to the caller and is mapped to 502 by GlobalExceptionHandler.
     *
     * driverId and finalFare are intentionally null at creation time.
     */
    public RideResponse createRide(CreateRideRequest request) {
        Instant now = Instant.now();

        Ride ride = new Ride();
        ride.setId(new ObjectId().toHexString());
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
        ride.setFinalFare(null);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);

        // Call Fare Service for estimate BEFORE persisting.
        // If this fails, FareServiceException propagates and no ride is saved.
        FareRequest fareRequest = new FareRequest(ride.getId(), request.getDistanceKm());
        FareResponse fareResponse = fareServiceClient.getFareEstimate(fareRequest);
        ride.setEstimatedFare(fareResponse.getTotalFare());

        Ride saved = rideRepository.save(ride);
        return toRideResponse(saved);
    }

    /**
     * Retrieves a ride by its rideId.
     *
     * @throws RideNotFoundException if ride does not exist (→ 404).
     */
    public RideResponse getRideById(String rideId) {
        return toRideResponse(findRide(rideId));
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
     * 3. Call Driver Service GET
     * /api/drivers/available?serviceArea={pickupLocation}.
     * 4. Select the FIRST eligible driver returned (deterministic, simple rule).
     * 5. Store only driverId in the ride.
     * 6. Transition status: REQUESTED → ASSIGNED.
     * 7. Update updatedAt timestamp.
     * 8. Persist and return RideResponse.
     *
     * @param rideId the ride to assign a driver to
     * @return RideResponse with status ASSIGNED and driverId populated
     * @throws RideNotFoundException                              if ride does not
     *                                                            exist
     * @throws InvalidRideStateException                          if ride is not in
     *                                                            REQUESTED status
     * @throws NoAvailableDriverException                         if no available
     *                                                            drivers are found
     * @throws com.ridelink.ride.exception.DriverServiceException if Driver Service
     *                                                            is unreachable
     */
    public RideResponse assignDriver(String rideId) {
        // Step 1: Load ride
        Ride ride = findRide(rideId);

        // Step 2: Validate ride is REQUESTED
        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateException(
                    "Cannot assign driver: ride status is " + ride.getStatus()
                            + ", expected REQUESTED");
        }

        // Step 3: Call Driver Service for available drivers
        // Use pickupLocation as the serviceArea query parameter
        String serviceArea = ride.getPickupLocation();
        List<AvailableDriverResponse> availableDrivers = driverServiceClient.getAvailableDrivers(serviceArea);

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

    public RideResponse acceptRide(String rideId) {
        return transitionRide(rideId, RideStatus.ASSIGNED, RideStatus.ACCEPTED, "accept");
    }

    public RideResponse startRide(String rideId) {
        return transitionRide(rideId, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, "start");
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = findRide(rideId);
        if (ride.getStatus() != RideStatus.REQUESTED
                && ride.getStatus() != RideStatus.ASSIGNED
                && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStateException(
                    "Cannot cancel ride: ride status is " + ride.getStatus()
                            + ", expected REQUESTED, ASSIGNED, or ACCEPTED");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setUpdatedAt(Instant.now());
        return toRideResponse(rideRepository.save(ride));
    }

    /**
     * Completes a ride by transitioning IN_PROGRESS → COMPLETED
     * and calling Fare Service for the final fare.
     *
     * Flow (API_CONTRACTS.md §5.8 and §6.3):
     * 1. Load ride by rideId (404 if not found).
     * 2. Validate ride is in IN_PROGRESS status (409 if not).
     * 3. Call Fare Service POST /api/fares/final with rideId and distanceKm.
     * 4. Store the returned totalFare as finalFare.
     * 5. Transition status: IN_PROGRESS → COMPLETED.
     * 6. Update updatedAt timestamp.
     * 7. Persist and return RideResponse.
     *
     * If Fare Service fails:
     * - Ride remains IN_PROGRESS (no status change).
     * - No invented finalFare is stored.
     * - FareServiceException propagates → 502 BAD_GATEWAY.
     *
     * @param rideId the ride to complete
     * @return RideResponse with status COMPLETED and finalFare populated
     * @throws RideNotFoundException                            if ride does not
     *                                                          exist
     * @throws InvalidRideStateException                        if ride is not in
     *                                                          IN_PROGRESS status
     * @throws com.ridelink.ride.exception.FareServiceException if Fare Service is
     *                                                          unreachable
     */
    public RideResponse completeRide(String rideId) {
        // Step 1: Load ride
        Ride ride = findRide(rideId);

        // Step 2: Validate ride is IN_PROGRESS
        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateException(
                    "Cannot complete ride: ride status is " + ride.getStatus()
                            + ", expected IN_PROGRESS");
        }

        // Step 3: Call Fare Service for final fare BEFORE modifying ride.
        // If this fails, FareServiceException propagates and ride stays IN_PROGRESS.
        FareRequest fareRequest = new FareRequest(rideId, ride.getDistanceKm());
        FareResponse fareResponse = fareServiceClient.getFinalFare(fareRequest);

        // Step 4: Store finalFare from Fare Service response
        ride.setFinalFare(fareResponse.getTotalFare());

        // Step 5: Transition status IN_PROGRESS → COMPLETED
        ride.setStatus(RideStatus.COMPLETED);

        // Step 6: Update timestamp
        ride.setUpdatedAt(Instant.now());

        // Step 7: Persist and return
        Ride saved = rideRepository.save(ride);
        return toRideResponse(saved);
    }

    private RideResponse transitionRide(String rideId,
                                        RideStatus expectedStatus,
                                        RideStatus targetStatus,
                                        String operation) {
        Ride ride = findRide(rideId);
        if (ride.getStatus() != expectedStatus) {
            throw new InvalidRideStateException(
                    "Cannot " + operation + " ride: ride status is " + ride.getStatus()
                            + ", expected " + expectedStatus);
        }

        ride.setStatus(targetStatus);
        ride.setUpdatedAt(Instant.now());
        return toRideResponse(rideRepository.save(ride));
    }

    private Ride findRide(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
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
