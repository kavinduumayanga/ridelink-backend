package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for RideController.
 * Uses WebMvcTest to test HTTP layer without starting full application.
 */
@WebMvcTest(RideController.class)
@AutoConfigureMockMvc(addFilters = false)
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- POST /api/rides ---

    @Test
    @DisplayName("POST /api/rides with valid request returns 201 and ride DTO")
    void createRide_validRequest_returns201() throws Exception {
        RideResponse response = buildSampleResponse("ride123");
        when(rideService.createRide(any(CreateRideRequest.class))).thenReturn(response);

        CreateRideRequest request = buildValidRequest();

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value("ride123"))
                .andExpect(jsonPath("$.passengerId").value("passenger123"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.driverId").doesNotExist())
                .andExpect(jsonPath("$.pickupLocation").value("Colombo Fort"))
                .andExpect(jsonPath("$.dropoffLocation").value("Bambalapitiya"))
                .andExpect(jsonPath("$.distanceKm").value(5.5));
    }

    @Test
    @DisplayName("POST /api/rides with missing passengerId returns 400")
    void createRide_missingPassengerId_returns400() throws Exception {
        CreateRideRequest request = buildValidRequest();
        request.setPassengerId(null);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/rides with negative distanceKm returns 400")
    void createRide_negativeDistance_returns400() throws Exception {
        CreateRideRequest request = buildValidRequest();
        request.setDistanceKm(-1.0);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/rides with zero distanceKm returns 400")
    void createRide_zeroDistance_returns400() throws Exception {
        CreateRideRequest request = buildValidRequest();
        request.setDistanceKm(0.0);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/rides with missing pickupLocation returns 400")
    void createRide_missingPickupLocation_returns400() throws Exception {
        CreateRideRequest request = buildValidRequest();
        request.setPickupLocation(null);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/rides with out-of-range latitude returns 400")
    void createRide_invalidLatitude_returns400() throws Exception {
        CreateRideRequest request = buildValidRequest();
        request.setPickupLatitude(91.0);

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // --- GET /api/rides/{rideId} ---

    @Test
    @DisplayName("GET /api/rides/{rideId} with existing ride returns 200")
    void getRide_found_returns200() throws Exception {
        RideResponse response = buildSampleResponse("ride123");
        when(rideService.getRideById("ride123")).thenReturn(response);

        mockMvc.perform(get("/api/rides/ride123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride123"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("GET /api/rides/{rideId} with non-existent ride returns 404")
    void getRide_notFound_returns404() throws Exception {
        when(rideService.getRideById("nonexistent"))
                .thenThrow(new RideNotFoundException("nonexistent"));

        mockMvc.perform(get("/api/rides/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // --- GET /api/rides/passenger/{passengerId} ---

    @Test
    @DisplayName("GET /api/rides/passenger/{passengerId} returns rides list")
    void getPassengerRides_found_returns200() throws Exception {
        RideResponse ride1 = buildSampleResponse("ride1");
        RideResponse ride2 = buildSampleResponse("ride2");
        when(rideService.getRidesByPassengerId("passenger123"))
                .thenReturn(List.of(ride1, ride2));

        mockMvc.perform(get("/api/rides/passenger/passenger123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rideId").value("ride1"))
                .andExpect(jsonPath("$[1].rideId").value("ride2"));
    }

    @Test
    @DisplayName("GET /api/rides/passenger/{passengerId} returns empty array when no rides")
    void getPassengerRides_empty_returns200() throws Exception {
        when(rideService.getRidesByPassengerId("unknown"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/rides/passenger/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- Helpers ---

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

    private RideResponse buildSampleResponse(String rideId) {
        RideResponse response = new RideResponse();
        response.setRideId(rideId);
        response.setPassengerId("passenger123");
        response.setDriverId(null);
        response.setPickupLocation("Colombo Fort");
        response.setDropoffLocation("Bambalapitiya");
        response.setPickupLatitude(6.9340);
        response.setPickupLongitude(79.8428);
        response.setDropoffLatitude(6.8942);
        response.setDropoffLongitude(79.8558);
        response.setDistanceKm(5.5);
        response.setStatus("REQUESTED");
        response.setEstimatedFare(null);
        response.setFinalFare(null);
        response.setCreatedAt(Instant.parse("2026-09-27T17:00:00Z"));
        response.setUpdatedAt(Instant.parse("2026-09-27T17:00:00Z"));
        return response;
    }
}
