package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareRequest;
import com.ridelink.ride.dto.FareResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.FareServiceException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Fare Service integration in RideService.
 * Covers:
 * - Fare estimation during ride creation (§6.2)
 * - Final fare during ride completion (§6.3)
 * - Failure consistency for both flows
 *
 * Uses Mockito mocks — Fare Service does NOT need to be running.
 */
@ExtendWith(MockitoExtension.class)
class RideFareIntegrationServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private FareServiceClient fareServiceClient;

    @InjectMocks
    private RideService rideService;

    // ================================================================
    // Fare Estimate during Ride Creation
    // ================================================================

    @Nested
    @DisplayName("Fare Estimate — createRide")
    class FareEstimateTests {

        @Test
        @DisplayName("Successful estimate: estimatedFare stored in ride from Fare Service response")
        void createRide_fareEstimateSuccess_estimatedFareStored() {
            FareResponse fareResponse = buildEstimateFareResponse(475.0);
            when(fareServiceClient.getFareEstimate(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
                Ride ride = inv.getArgument(0);
                ride.setId("ride1");
                return ride;
            });

            RideResponse response = rideService.createRide(buildValidRequest());

            assertEquals(475.0, response.getEstimatedFare());
            assertEquals("REQUESTED", response.getStatus());
            verify(fareServiceClient).getFareEstimate(any(FareRequest.class));
            verify(rideRepository).save(any(Ride.class));
        }

        @Test
        @DisplayName("Returned estimatedFare matches Fare Service totalFare exactly")
        void createRide_fareEstimate_matchesTotalFare() {
            FareResponse fareResponse = buildEstimateFareResponse(825.0);
            when(fareServiceClient.getFareEstimate(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
                Ride ride = inv.getArgument(0);
                ride.setId("ride2");
                return ride;
            });

            RideResponse response = rideService.createRide(buildValidRequest());

            assertEquals(825.0, response.getEstimatedFare());
        }

        @Test
        @DisplayName("Fare Service failure prevents ride persistence")
        void createRide_fareServiceFails_rideNotSaved() {
            when(fareServiceClient.getFareEstimate(any(FareRequest.class)))
                    .thenThrow(new FareServiceException("Connection refused"));

            assertThrows(FareServiceException.class, () ->
                    rideService.createRide(buildValidRequest())
            );

            // Ride must NOT be saved when fare estimation fails
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("No locally invented fare — estimatedFare comes from Fare Service only")
        void createRide_fareFromServiceOnly_notInvented() {
            // Fare Service returns a specific value
            FareResponse fareResponse = buildEstimateFareResponse(999.99);
            when(fareServiceClient.getFareEstimate(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
                Ride ride = inv.getArgument(0);
                ride.setId("ride3");
                return ride;
            });

            RideResponse response = rideService.createRide(buildValidRequest());

            // Must match what Fare Service returned, not any local calculation
            assertEquals(999.99, response.getEstimatedFare());
        }

        @Test
        @DisplayName("Fare estimate uses the stable Ride Service rideId")
        void createRide_fareEstimateUsesStableRideId() {
            when(fareServiceClient.getFareEstimate(any(FareRequest.class)))
                    .thenReturn(buildEstimateFareResponse(475.0));
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

            RideResponse response = rideService.createRide(buildValidRequest());

            ArgumentCaptor<FareRequest> requestCaptor = ArgumentCaptor.forClass(FareRequest.class);
            verify(fareServiceClient).getFareEstimate(requestCaptor.capture());
            assertEquals(response.getRideId(), requestCaptor.getValue().getRideId());
            assertTrue(requestCaptor.getValue().getRideId().matches("[0-9a-f]{24}"));
        }
    }

    // ================================================================
    // Final Fare during Ride Completion
    // ================================================================

    @Nested
    @DisplayName("Final Fare — completeRide")
    class FinalFareTests {

        @Test
        @DisplayName("Successful final fare: status becomes COMPLETED and finalFare stored")
        void completeRide_fareSuccess_completedWithFinalFare() {
            Ride ride = buildInProgressRide("ride1");
            when(rideRepository.findById("ride1")).thenReturn(Optional.of(ride));
            FareResponse fareResponse = buildFinalFareResponse(860.0);
            when(fareServiceClient.getFinalFare(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

            RideResponse response = rideService.completeRide("ride1");

            assertEquals("COMPLETED", response.getStatus());
            assertEquals(860.0, response.getFinalFare());
            verify(fareServiceClient).getFinalFare(any(FareRequest.class));
            verify(rideRepository).save(any(Ride.class));
        }

        @Test
        @DisplayName("Returned finalFare matches Fare Service totalFare exactly")
        void completeRide_finalFareMatchesTotalFare() {
            Ride ride = buildInProgressRide("ride2");
            when(rideRepository.findById("ride2")).thenReturn(Optional.of(ride));
            FareResponse fareResponse = buildFinalFareResponse(1250.0);
            when(fareServiceClient.getFinalFare(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

            RideResponse response = rideService.completeRide("ride2");

            assertEquals(1250.0, response.getFinalFare());
        }

        @Test
        @DisplayName("Status becomes COMPLETED only after successful fare response")
        void completeRide_statusOnlyAfterFareSuccess() {
            Ride ride = buildInProgressRide("ride3");
            when(rideRepository.findById("ride3")).thenReturn(Optional.of(ride));
            FareResponse fareResponse = buildFinalFareResponse(500.0);
            when(fareServiceClient.getFinalFare(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

            rideService.completeRide("ride3");

            verify(rideRepository).save(argThat(savedRide ->
                    savedRide.getStatus() == RideStatus.COMPLETED
            ));
        }

        @Test
        @DisplayName("Fare Service failure leaves ride IN_PROGRESS")
        void completeRide_fareServiceFails_rideStaysInProgress() {
            Ride ride = buildInProgressRide("ride4");
            when(rideRepository.findById("ride4")).thenReturn(Optional.of(ride));
            when(fareServiceClient.getFinalFare(any(FareRequest.class)))
                    .thenThrow(new FareServiceException("Connection refused"));

            assertThrows(FareServiceException.class, () ->
                    rideService.completeRide("ride4")
            );

            // Ride must NOT be saved when fare calculation fails
            verify(rideRepository, never()).save(any(Ride.class));
            // Ride object should remain unchanged
            assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());
        }

        @Test
        @DisplayName("Fare Service failure does not store finalFare")
        void completeRide_fareServiceFails_noFinalFareStored() {
            Ride ride = buildInProgressRide("ride5");
            when(rideRepository.findById("ride5")).thenReturn(Optional.of(ride));
            when(fareServiceClient.getFinalFare(any(FareRequest.class)))
                    .thenThrow(new FareServiceException("Timeout"));

            assertThrows(FareServiceException.class, () ->
                    rideService.completeRide("ride5")
            );

            assertNull(ride.getFinalFare());
        }

        @Test
        @DisplayName("Complete from REQUESTED → 409 CONFLICT")
        void completeRide_fromRequested_throws409() {
            Ride ride = buildRideWithStatus("ride6", RideStatus.REQUESTED);
            when(rideRepository.findById("ride6")).thenReturn(Optional.of(ride));

            assertThrows(InvalidRideStateException.class, () ->
                    rideService.completeRide("ride6")
            );
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Complete from COMPLETED → 409 CONFLICT")
        void completeRide_fromCompleted_throws409() {
            Ride ride = buildRideWithStatus("ride7", RideStatus.COMPLETED);
            when(rideRepository.findById("ride7")).thenReturn(Optional.of(ride));

            assertThrows(InvalidRideStateException.class, () ->
                    rideService.completeRide("ride7")
            );
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("Complete from CANCELLED → 409 CONFLICT")
        void completeRide_fromCancelled_throws409() {
            Ride ride = buildRideWithStatus("ride8", RideStatus.CANCELLED);
            when(rideRepository.findById("ride8")).thenReturn(Optional.of(ride));

            assertThrows(InvalidRideStateException.class, () ->
                    rideService.completeRide("ride8")
            );
            verify(rideRepository, never()).save(any(Ride.class));
        }

        @Test
        @DisplayName("updatedAt changes after completion")
        void completeRide_updatedAtChanges() {
            Instant originalUpdatedAt = Instant.parse("2026-01-01T00:00:00Z");
            Ride ride = buildInProgressRide("ride9");
            ride.setUpdatedAt(originalUpdatedAt);
            when(rideRepository.findById("ride9")).thenReturn(Optional.of(ride));
            FareResponse fareResponse = buildFinalFareResponse(500.0);
            when(fareServiceClient.getFinalFare(any(FareRequest.class))).thenReturn(fareResponse);
            when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

            RideResponse response = rideService.completeRide("ride9");

            assertNotEquals(originalUpdatedAt, response.getUpdatedAt());
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private CreateRideRequest buildValidRequest() {
        CreateRideRequest request = new CreateRideRequest();
        request.setPassengerId("passenger123");
        request.setPickupLocation("Colombo Fort");
        request.setDropoffLocation("Bambalapitiya");
        request.setPickupLatitude(6.9340);
        request.setPickupLongitude(79.8428);
        request.setDropoffLatitude(6.8942);
        request.setDropoffLongitude(79.8558);
        request.setDistanceKm(5.5);
        return request;
    }

    private Ride buildInProgressRide(String id) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerId("passenger123");
        ride.setDriverId("driver1");
        ride.setPickupLocation("Colombo Fort");
        ride.setDropoffLocation("Bambalapitiya");
        ride.setPickupLatitude(6.9340);
        ride.setPickupLongitude(79.8428);
        ride.setDropoffLatitude(6.8942);
        ride.setDropoffLongitude(79.8558);
        ride.setDistanceKm(5.5);
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setEstimatedFare(475.0);
        ride.setFinalFare(null);
        ride.setCreatedAt(Instant.now());
        ride.setUpdatedAt(Instant.now());
        return ride;
    }

    private Ride buildRideWithStatus(String id, RideStatus status) {
        Ride ride = buildInProgressRide(id);
        ride.setStatus(status);
        return ride;
    }

    private FareResponse buildEstimateFareResponse(Double totalFare) {
        FareResponse response = new FareResponse();
        response.setFareId("fare-est-1");
        response.setRideId("ride-estimate");
        response.setFareType("ESTIMATE");
        response.setDistanceKm(5.5);
        response.setBaseFare(200.0);
        response.setRatePerKm(50.0);
        response.setTotalFare(totalFare);
        return response;
    }

    private FareResponse buildFinalFareResponse(Double totalFare) {
        FareResponse response = new FareResponse();
        response.setFareId("fare-final-1");
        response.setRideId("ride1");
        response.setFareType("FINAL");
        response.setDistanceKm(5.5);
        response.setBaseFare(200.0);
        response.setRatePerKm(50.0);
        response.setTotalFare(totalFare);
        return response;
    }
}
