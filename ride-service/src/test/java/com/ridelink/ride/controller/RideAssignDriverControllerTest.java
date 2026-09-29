package com.ridelink.ride.controller;

import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.DriverServiceException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.NoAvailableDriverException;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for PATCH /api/rides/{rideId}/assign.
 * Tests HTTP layer error mapping without starting Driver Service.
 */
@WebMvcTest(RideController.class)
@AutoConfigureMockMvc(addFilters = false)
class RideAssignDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    // --- Happy Path ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign with valid REQUESTED ride returns 200 with ASSIGNED status")
    void assignDriver_success_returns200() throws Exception {
        RideResponse response = buildAssignedResponse("ride1", "driver1");
        when(rideService.assignDriver("ride1")).thenReturn(response);

        mockMvc.perform(patch("/api/rides/ride1/assign"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride1"))
                .andExpect(jsonPath("$.driverId").value("driver1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    // --- Ride Not Found ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign with non-existent ride returns 404")
    void assignDriver_rideNotFound_returns404() throws Exception {
        when(rideService.assignDriver("nonexistent"))
                .thenThrow(new RideNotFoundException("nonexistent"));

        mockMvc.perform(patch("/api/rides/nonexistent/assign"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // --- Invalid State Transition ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign from ASSIGNED returns 409")
    void assignDriver_fromAssigned_returns409() throws Exception {
        when(rideService.assignDriver("ride2"))
                .thenThrow(new InvalidRideStateException(
                        "Cannot assign driver: ride status is ASSIGNED, expected REQUESTED"));

        mockMvc.perform(patch("/api/rides/ride2/assign"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign from ACCEPTED returns 409")
    void assignDriver_fromAccepted_returns409() throws Exception {
        when(rideService.assignDriver("ride3"))
                .thenThrow(new InvalidRideStateException(
                        "Cannot assign driver: ride status is ACCEPTED, expected REQUESTED"));

        mockMvc.perform(patch("/api/rides/ride3/assign"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign from COMPLETED returns 409")
    void assignDriver_fromCompleted_returns409() throws Exception {
        when(rideService.assignDriver("ride4"))
                .thenThrow(new InvalidRideStateException(
                        "Cannot assign driver: ride status is COMPLETED, expected REQUESTED"));

        mockMvc.perform(patch("/api/rides/ride4/assign"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // --- No Available Drivers ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign with no available drivers returns 404")
    void assignDriver_noDrivers_returns404() throws Exception {
        when(rideService.assignDriver("ride5"))
                .thenThrow(new NoAvailableDriverException("Colombo Fort"));

        mockMvc.perform(patch("/api/rides/ride5/assign"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No available drivers found in area: Colombo Fort"));
    }

    // --- Driver Service Unavailable ---

    @Test
    @DisplayName("PATCH /api/rides/{rideId}/assign when Driver Service is down returns 502")
    void assignDriver_driverServiceDown_returns502() throws Exception {
        when(rideService.assignDriver("ride6"))
                .thenThrow(new DriverServiceException("Connection refused"));

        mockMvc.perform(patch("/api/rides/ride6/assign"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Driver Service is currently unavailable"));
    }

    // --- Helper ---

    private RideResponse buildAssignedResponse(String rideId, String driverId) {
        RideResponse response = new RideResponse();
        response.setRideId(rideId);
        response.setPassengerId("passenger123");
        response.setDriverId(driverId);
        response.setPickupLocation("Colombo Fort");
        response.setDropoffLocation("Bambalapitiya");
        response.setPickupLatitude(6.9340);
        response.setPickupLongitude(79.8428);
        response.setDropoffLatitude(6.8942);
        response.setDropoffLongitude(79.8558);
        response.setDistanceKm(5.5);
        response.setStatus("ASSIGNED");
        response.setEstimatedFare(null);
        response.setFinalFare(null);
        response.setCreatedAt(Instant.parse("2026-09-27T17:00:00Z"));
        response.setUpdatedAt(Instant.parse("2026-09-27T17:01:00Z"));
        return response;
    }
}
