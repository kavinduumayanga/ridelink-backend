package com.ridelink.ride.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ridelink.ride.config.SecurityConfig;
import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.controller.RideController;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RideController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, RideAuthorization.class,
        RideSecurityTest.SwaggerProbeController.class})
@TestPropertySource(properties = "security.jwt.secret=ride-security-test-secret-32-bytes")
class RideSecurityTest {

    private static final String TEST_SECRET = "ride-security-test-secret-32-bytes";
    private static final String PASSENGER_ID = "passenger-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @MockitoBean
    private RideRepository rideRepository;

    @MockitoBean
    private DriverServiceClient driverServiceClient;

    @Test
    @DisplayName("Protected ride endpoint without JWT returns 401")
    void protectedEndpoint_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/rides/ride-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/rides/ride-1"));
    }

    @Test
    @DisplayName("Malformed JWT returns 401")
    void protectedEndpoint_withMalformedJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/rides/ride-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("JWT with invalid signature returns 401")
    void protectedEndpoint_withInvalidSignature_returns401() throws Exception {
        Instant now = Instant.now();
        String token = tokenSignedWith(
                "different-test-signing-secret-32b",
                PASSENGER_ID,
                "PASSENGER",
                now.minusSeconds(5),
                now.plusSeconds(600));

        mockMvc.perform(get("/api/rides/ride-1").headers(bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Expired JWT returns 401")
    void protectedEndpoint_withExpiredJwt_returns401() throws Exception {
        String token = token(PASSENGER_ID, "PASSENGER",
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(300));

        mockMvc.perform(get("/api/rides/ride-1").headers(bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Valid PASSENGER JWT can create own ride")
    void passenger_canCreateOwnRide() throws Exception {
        RideResponse response = new RideResponse();
        response.setRideId("ride-1");
        response.setPassengerId(PASSENGER_ID);
        response.setStatus("REQUESTED");
        when(rideService.createRide(any())).thenReturn(response);

        mockMvc.perform(post("/api/rides")
                        .headers(bearer(validToken(PASSENGER_ID, "PASSENGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest(PASSENGER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passengerId").value(PASSENGER_ID));
    }

    @Test
    @DisplayName("DRIVER role cannot create a ride")
    void wrongRole_returns403() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .headers(bearer(validToken("driver-account-1", "DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest("driver-account-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Passenger cannot assign another passenger's ride")
    void passenger_cannotAssignAnotherPassengersRide() throws Exception {
        Ride ride = new Ride();
        ride.setId("ride-1");
        ride.setPassengerId("passenger-2");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        mockMvc.perform(patch("/api/rides/ride-1/assign")
                        .headers(bearer(validToken(PASSENGER_ID, "PASSENGER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(rideService, never()).assignDriver(any());
    }

    @Test
    @DisplayName("ADMIN can retrieve any passenger's ride history")
    void admin_canRetrievePassengerRideHistory() throws Exception {
        when(rideService.getRidesByPassengerId("passenger-2"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/rides/passenger/passenger-2")
                        .headers(bearer(validToken("admin-1", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DRIVER cannot complete a ride assigned to another driver account")
    void driver_cannotCompleteAnotherDriversRide() throws Exception {
        Ride ride = new Ride();
        ride.setId("ride-1");
        ride.setDriverId("driver-profile-2");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        AvailableDriverResponse assignedDriver = new AvailableDriverResponse();
        assignedDriver.setDriverId("driver-profile-2");
        assignedDriver.setAccountId("driver-account-2");
        when(driverServiceClient.getDriver("driver-profile-2")).thenReturn(assignedDriver);

        mockMvc.perform(patch("/api/rides/ride-1/complete")
                        .headers(bearer(validToken("driver-account-1", "DRIVER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        verify(rideService, never()).completeRide(any());
    }

    @Test
    @DisplayName("Assigned DRIVER account can complete its ride")
    void assignedDriver_canCompleteRide() throws Exception {
        Ride ride = new Ride();
        ride.setId("ride-1");
        ride.setDriverId("driver-profile-1");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));

        AvailableDriverResponse assignedDriver = new AvailableDriverResponse();
        assignedDriver.setDriverId("driver-profile-1");
        assignedDriver.setAccountId("driver-account-1");
        when(driverServiceClient.getDriver("driver-profile-1")).thenReturn(assignedDriver);

        RideResponse response = new RideResponse();
        response.setRideId("ride-1");
        response.setDriverId("driver-profile-1");
        response.setStatus("COMPLETED");
        when(rideService.completeRide("ride-1")).thenReturn(response);

        mockMvc.perform(patch("/api/rides/ride-1/complete")
                        .headers(bearer(validToken("driver-account-1", "DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Swagger UI remains public")
    void swaggerUi_isPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs/security-test"))
                .andExpect(status().isOk());
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String validToken(String subject, String role) throws Exception {
        Instant now = Instant.now();
        return token(subject, role, now.minusSeconds(5), now.plusSeconds(600));
    }

    private String token(String subject, String role, Instant issuedAt, Instant expiresAt) throws Exception {
        return tokenSignedWith(TEST_SECRET, subject, role, issuedAt, expiresAt);
    }

    private String tokenSignedWith(String secret,
                                   String subject,
                                   String role,
                                   Instant issuedAt,
                                   Instant expiresAt) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("role", role)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes()));
        return jwt.serialize();
    }

    private String validCreateRequest(String passengerId) {
        return """
                {
                  "passengerId": "%s",
                  "pickupLocation": "Colombo Fort",
                  "dropoffLocation": "Bambalapitiya",
                  "pickupLatitude": 6.9340,
                  "pickupLongitude": 79.8428,
                  "dropoffLatitude": 6.8942,
                  "dropoffLongitude": 79.8558,
                  "distanceKm": 5.5
                }
                """.formatted(passengerId);
    }

    @RestController
    static class SwaggerProbeController {

        @GetMapping("/v3/api-docs/security-test")
        String apiDocsProbe() {
            return "public";
        }
    }
}
