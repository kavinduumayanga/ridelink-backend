package com.ridelink.ride.controller;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.config.SecurityConfig;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.JwtAccessDeniedHandler;
import com.ridelink.ride.security.JwtAuthenticationEntryPoint;
import com.ridelink.ride.security.RideAuthorization;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, RideAuthorization.class})
@WithMockUser(username = "driver-account-1", roles = "DRIVER")
class RideLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @MockitoBean
    private RideRepository rideRepository;

    @MockitoBean
    private DriverServiceClient driverServiceClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void authorizeAssignedDriver() {
        Ride ride = new Ride();
        ride.setId("ride-1");
        ride.setPassengerId("passenger-1");
        ride.setDriverId("driver-1");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        AvailableDriverResponse driver = new AvailableDriverResponse();
        driver.setDriverId("driver-1");
        driver.setAccountId("driver-account-1");
        when(driverServiceClient.getDriver("driver-1")).thenReturn(driver);
    }

    @Test
    void acceptEndpoint_returnsAcceptedRide() throws Exception {
        when(rideService.acceptRide("ride-1")).thenReturn(response("ACCEPTED"));

        mockMvc.perform(patch("/api/rides/ride-1/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void startEndpoint_returnsInProgressRide() throws Exception {
        when(rideService.startRide("ride-1")).thenReturn(response("IN_PROGRESS"));

        mockMvc.perform(patch("/api/rides/ride-1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "passenger-1", roles = "PASSENGER")
    void cancelEndpoint_returnsCancelledRide() throws Exception {
        when(rideService.cancelRide("ride-1")).thenReturn(response("CANCELLED"));

        mockMvc.perform(patch("/api/rides/ride-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private RideResponse response(String status) {
        RideResponse response = new RideResponse();
        response.setRideId("ride-1");
        response.setStatus(status);
        return response;
    }
}
