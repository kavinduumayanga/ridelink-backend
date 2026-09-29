package com.ridelink.ride.controller;

import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.FareServiceException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for Fare Service integration endpoints.
 * Covers:
 * - PATCH /api/rides/{rideId}/complete (HTTP error mapping)
 * - POST /api/rides when Fare Service fails (HTTP error mapping)
 */
@WebMvcTest(RideController.class)
@AutoConfigureMockMvc(addFilters = false)
class RideFareIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    // --- PATCH /api/rides/{rideId}/complete ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/complete success returns 200 with COMPLETED status and finalFare")
    void completeRide_success_returns200() throws Exception {
        RideResponse response = buildCompletedResponse("ride1", 860.0);
        when(rideService.completeRide("ride1")).thenReturn(response);

        mockMvc.perform(patch("/api/rides/ride1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride1"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalFare").value(860.0));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/complete with non-existent ride returns 404")
    void completeRide_rideNotFound_returns404() throws Exception {
        when(rideService.completeRide("nonexistent"))
                .thenThrow(new RideNotFoundException("nonexistent"));

        mockMvc.perform(patch("/api/rides/nonexistent/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/complete from REQUESTED returns 409")
    void completeRide_fromRequested_returns409() throws Exception {
        when(rideService.completeRide("ride2"))
                .thenThrow(new InvalidRideStateException(
                        "Cannot complete ride: ride status is REQUESTED, expected IN_PROGRESS"));

        mockMvc.perform(patch("/api/rides/ride2/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/complete when Fare Service is down returns 502")
    void completeRide_fareServiceDown_returns502() throws Exception {
        when(rideService.completeRide("ride3"))
                .thenThrow(new FareServiceException("Connection refused"));

        mockMvc.perform(patch("/api/rides/ride3/complete"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Fare Service is currently unavailable"));
    }

    // --- POST /api/rides when Fare Service fails ---

    @Test
    @DisplayName("POST /api/rides when Fare Service fails returns 502")
    void createRide_fareServiceDown_returns502() throws Exception {
        when(rideService.createRide(any()))
                .thenThrow(new FareServiceException("Connection refused"));

        String requestBody = """
                {
                  "passengerId": "passenger123",
                  "pickupLocation": "Colombo Fort",
                  "dropoffLocation": "Bambalapitiya",
                  "pickupLatitude": 6.9340,
                  "pickupLongitude": 79.8428,
                  "dropoffLatitude": 6.8942,
                  "dropoffLongitude": 79.8558,
                  "distanceKm": 5.5
                }
                """;

        mockMvc.perform(post("/api/rides")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Fare Service is currently unavailable"));
    }

    // --- Helper ---

    private RideResponse buildCompletedResponse(String rideId, Double finalFare) {
        RideResponse response = new RideResponse();
        response.setRideId(rideId);
        response.setPassengerId("passenger123");
        response.setDriverId("driver1");
        response.setPickupLocation("Colombo Fort");
        response.setDropoffLocation("Bambalapitiya");
        response.setPickupLatitude(6.9340);
        response.setPickupLongitude(79.8428);
        response.setDropoffLatitude(6.8942);
        response.setDropoffLongitude(79.8558);
        response.setDistanceKm(5.5);
        response.setStatus("COMPLETED");
        response.setEstimatedFare(475.0);
        response.setFinalFare(finalFare);
        response.setCreatedAt(Instant.parse("2026-09-27T17:00:00Z"));
        response.setUpdatedAt(Instant.parse("2026-09-27T17:30:00Z"));
        return response;
    }
}
