package com.ridelink.driver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.config.SecurityConfig;
import com.ridelink.driver.controller.DriverController;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.exception.GlobalExceptionHandler;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Key;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests JWT authentication and role-based authorization for Driver Service endpoints.
 * Uses real SecurityConfig and JwtAuthenticationFilter with a test JWT secret.
 */
@WebMvcTest(controllers = DriverController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=TestSecretKeyThatIsAtLeast32BytesLong!!")
class SecurityIntegrationTest {

    private static final String TEST_SECRET = "TestSecretKeyThatIsAtLeast32BytesLong!!";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverService driverService;

    @MockBean
    private VehicleService vehicleService;

    private String driverToken;
    private String passengerToken;
    private String adminToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        driverToken = generateToken("user-123", "DRIVER", 3600000);
        passengerToken = generateToken("user-456", "PASSENGER", 3600000);
        adminToken = generateToken("admin-789", "ADMIN", 3600000);
        expiredToken = generateExpiredToken("user-123", "DRIVER");
    }

    // ========== 401 UNAUTHENTICATED TESTS ==========

    @Test
    void testNoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/driver-789"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testAvailableDriversNoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testInvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void testExpiredToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void testMalformedAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "NotBearer some-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void testWrongSecret_ReturnsUnauthorized() throws Exception {
        Key wrongKey = Keys.hmacShaKeyFor("DifferentSecretKeyThatIsAtLeast32Bytes!!".getBytes());
        String wrongToken = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "DRIVER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + wrongToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void testMissingRequiredClaims_ReturnsUnauthorized() throws Exception {
        String tokenNoRole = Jwts.builder()
                .setSubject("user-123")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + tokenNoRole))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void testInvalidRoleClaim_ReturnsUnauthorized() throws Exception {
        String tokenInvalidRole = Jwts.builder()
                .setSubject("user-123")
                .claim("role", "UNKNOWN_ROLE")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + tokenInvalidRole))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    // ========== 403 FORBIDDEN TESTS ==========

    @Test
    void testPassengerCannotCreateDriver_ReturnsForbidden() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest("user-456", "DL-123456", "Colombo");

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testPassengerCannotUpdateAvailability_ReturnsForbidden() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);

        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void testPassengerCannotUpdateLocation_ReturnsForbidden() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void testPassengerCannotCreateVehicle_ReturnsForbidden() throws Exception {
        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        mockMvc.perform(post("/api/drivers/driver-789/vehicle")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void testPassengerCannotUpdateVehicle_ReturnsForbidden() throws Exception {
        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        mockMvc.perform(put("/api/drivers/driver-789/vehicle")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ========== ADMIN ROLE TESTS (per frozen contract) ==========

    @Test
    void testAdminCannotCreateDriver_ReturnsForbidden() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest("admin-789", "DL-123456", "Colombo");

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testAdminCannotUpdateAvailability_ReturnsForbidden() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);

        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void testAdminCannotUpdateLocation_ReturnsForbidden() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // ========== OWNERSHIP VIOLATION (403) TESTS ==========

    @Test
    void testDriverCannotCreateProfileForAnotherAccountId_ReturnsForbidden() throws Exception {
        when(driverService.createDriver(any(CreateDriverRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this driver resource"));

        CreateDriverRequest request = new CreateDriverRequest("other-user-999", "DL-123456", "Colombo");

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testDriverCannotUpdateAnotherDriverAvailability_ReturnsForbidden() throws Exception {
        when(driverService.updateAvailability(eq("other-driver-999"), any(UpdateAvailabilityRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this driver resource"));

        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);

        mockMvc.perform(patch("/api/drivers/other-driver-999/availability")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testDriverCannotUpdateAnotherDriverLocation_ReturnsForbidden() throws Exception {
        when(driverService.updateLocation(eq("other-driver-999"), any(UpdateLocationRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this driver resource"));

        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);

        mockMvc.perform(patch("/api/drivers/other-driver-999/location")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testDriverCannotCreateVehicleForAnotherDriver_ReturnsForbidden() throws Exception {
        when(vehicleService.createVehicle(eq("other-driver-999"), any(com.ridelink.driver.dto.VehicleRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this driver resource"));

        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        mockMvc.perform(post("/api/drivers/other-driver-999/vehicle")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testDriverCannotUpdateVehicleForAnotherDriver_ReturnsForbidden() throws Exception {
        when(vehicleService.updateVehicle(eq("other-driver-999"), any(com.ridelink.driver.dto.VehicleRequest.class)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this driver resource"));

        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        mockMvc.perform(put("/api/drivers/other-driver-999/vehicle")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    // ========== AUTHORIZED ACCESS TESTS ==========

    @Test
    void testDriverCanCreateProfile() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest("user-123", "DL-123456", "Colombo");
        DriverResponse response = new DriverResponse(
                "driver-789", "user-123", "DL-123456", "Colombo",
                DriverAvailability.UNAVAILABLE, 0.0, 0.0
        );

        when(driverService.createDriver(any(CreateDriverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value("driver-789"));
    }

    @Test
    void testDriverCanUpdateAvailability() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);
        DriverResponse response = new DriverResponse(
                "driver-789", "user-123", "DL-123456", "Colombo",
                DriverAvailability.AVAILABLE, 6.9271, 79.8612
        );

        when(driverService.updateAvailability(eq("driver-789"), any(UpdateAvailabilityRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));
    }

    @Test
    void testDriverCanUpdateLocation() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);
        DriverResponse response = new DriverResponse(
                "driver-789", "user-123", "DL-123456", "Colombo",
                DriverAvailability.AVAILABLE, 6.9271, 79.8612
        );

        when(driverService.updateLocation(eq("driver-789"), any(UpdateLocationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(6.9271));
    }

    @Test
    void testDriverCanCreateVehicle() throws Exception {
        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);
        com.ridelink.driver.dto.VehicleResponse response = new com.ridelink.driver.dto.VehicleResponse(
                "veh-100", "driver-789", "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        when(vehicleService.createVehicle(eq("driver-789"), any(com.ridelink.driver.dto.VehicleRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/drivers/driver-789/vehicle")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value("veh-100"));
    }

    @Test
    void testDriverCanUpdateVehicle() throws Exception {
        com.ridelink.driver.dto.VehicleRequest request = new com.ridelink.driver.dto.VehicleRequest(
                "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);
        com.ridelink.driver.dto.VehicleResponse response = new com.ridelink.driver.dto.VehicleResponse(
                "veh-100", "driver-789", "ABC-1234", "Toyota", "Prius", 2022, "White", com.ridelink.driver.domain.VehicleType.CAR);

        when(vehicleService.updateVehicle(eq("driver-789"), any(com.ridelink.driver.dto.VehicleRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/drivers/driver-789/vehicle")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value("veh-100"));
    }

    @Test
    void testAnyAuthenticatedUserCanGetDriver() throws Exception {
        DriverResponse response = new DriverResponse(
                "driver-789", "user-123", "DL-123456", "Colombo",
                DriverAvailability.AVAILABLE, 6.9271, 79.8612
        );

        when(driverService.getDriverById("driver-789")).thenReturn(response);

        // PASSENGER can read driver profile
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"));

        // DRIVER can read driver profile
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"));

        // ADMIN can read driver profile
        mockMvc.perform(get("/api/drivers/driver-789")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"));
    }

    @Test
    void testAnyAuthenticatedUserCanGetAvailableDrivers() throws Exception {
        when(driverService.getAvailableDrivers("Colombo")).thenReturn(java.util.List.of());

        // PASSENGER can list available drivers
        mockMvc.perform(get("/api/drivers/available")
                        .header("Authorization", "Bearer " + passengerToken)
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk());

        // DRIVER can list available drivers
        mockMvc.perform(get("/api/drivers/available")
                        .header("Authorization", "Bearer " + driverToken)
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk());

        // ADMIN can list available drivers
        mockMvc.perform(get("/api/drivers/available")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk());
    }

    // ========== SWAGGER PUBLIC ACCESS ==========

    @Test
    void testSwaggerUiAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status != 401 && status != 403,
                            "Swagger endpoint should be publicly accessible without 401/403 (got " + status + ")"
                    );
                });
    }

    @Test
    void testApiDocsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status != 401 && status != 403,
                            "API docs endpoint should be publicly accessible without 401/403 (got " + status + ")"
                    );
                });
    }

    // ========== HELPER METHODS ==========

    private String generateToken(String userId, String role, long validityMs) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
}
