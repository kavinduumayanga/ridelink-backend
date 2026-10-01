package com.ridelink.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.config.SecurityConfig;
import com.ridelink.driver.domain.DriverAvailability;
import com.ridelink.driver.domain.VehicleType;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.DriverResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.dto.VehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.exception.DuplicateResourceException;
import com.ridelink.driver.exception.GlobalExceptionHandler;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.security.JwtAuthenticationFilter;
import com.ridelink.driver.security.JwtTokenProvider;
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=TestSecretKeyThatIsAtLeast32BytesLong!!")
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverService driverService;

    @MockBean
    private VehicleService vehicleService;

    @Test
    @WithMockUser(username = "acc-123", roles = "DRIVER")
    void testCreateDriver_Success() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest("acc-123", "DL-123456", "Colombo");
        DriverResponse response = new DriverResponse(
                "driver-789",
                "acc-123",
                "DL-123456",
                "Colombo",
                DriverAvailability.UNAVAILABLE,
                0.0,
                0.0
        );

        when(driverService.createDriver(any(CreateDriverRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.accountId").value("acc-123"))
                .andExpect(jsonPath("$.licenseNumber").value("DL-123456"))
                .andExpect(jsonPath("$.serviceArea").value("Colombo"))
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.latitude").value(0.0))
                .andExpect(jsonPath("$.longitude").value(0.0));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testCreateDriver_ValidationError() throws Exception {
        CreateDriverRequest invalidRequest = new CreateDriverRequest("", "", "");

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "acc-123", roles = "DRIVER")
    void testCreateDriver_DuplicateProfile_Conflict() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest("acc-123", "DL-123456", "Colombo");

        when(driverService.createDriver(any(CreateDriverRequest.class)))
                .thenThrow(new DuplicateResourceException("Driver profile already exists for accountId: acc-123"));

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Driver profile already exists for accountId: acc-123"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetDriverById_Success() throws Exception {
        DriverResponse response = new DriverResponse(
                "driver-789",
                "acc-123",
                "DL-123456",
                "Colombo",
                DriverAvailability.AVAILABLE,
                6.9271,
                79.8612
        );

        when(driverService.getDriverById("driver-789")).thenReturn(response);

        mockMvc.perform(get("/api/drivers/driver-789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.accountId").value("acc-123"))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.latitude").value(6.9271))
                .andExpect(jsonPath("$.longitude").value(79.8612));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetDriverById_NotFound() throws Exception {
        when(driverService.getDriverById("driver-999"))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: driver-999"));

        mockMvc.perform(get("/api/drivers/driver-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Driver not found with id: driver-999"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testCreateVehicle_Success() throws Exception {
        VehicleRequest request = new VehicleRequest("ABC-1234", "Toyota", "Prius", 2022, "White", VehicleType.CAR);
        VehicleResponse response = new VehicleResponse(
                "veh-100",
                "driver-789",
                "ABC-1234",
                "Toyota",
                "Prius",
                2022,
                "White",
                VehicleType.CAR
        );

        when(vehicleService.createVehicle(eq("driver-789"), any(VehicleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/drivers/driver-789/vehicle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value("veh-100"))
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.licensePlate").value("ABC-1234"))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Prius"))
                .andExpect(jsonPath("$.year").value(2022))
                .andExpect(jsonPath("$.color").value("White"))
                .andExpect(jsonPath("$.vehicleType").value("CAR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testCreateVehicle_DriverNotFound() throws Exception {
        VehicleRequest request = new VehicleRequest("ABC-1234", "Toyota", "Prius", 2022, "White", VehicleType.CAR);

        when(vehicleService.createVehicle(eq("driver-999"), any(VehicleRequest.class)))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: driver-999"));

        mockMvc.perform(post("/api/drivers/driver-999/vehicle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateVehicle_Success() throws Exception {
        VehicleRequest request = new VehicleRequest("ABC-9999", "Toyota", "Aqua", 2023, "Silver", VehicleType.CAR);
        VehicleResponse response = new VehicleResponse(
                "veh-100",
                "driver-789",
                "ABC-9999",
                "Toyota",
                "Aqua",
                2023,
                "Silver",
                VehicleType.CAR
        );

        when(vehicleService.updateVehicle(eq("driver-789"), any(VehicleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/drivers/driver-789/vehicle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value("veh-100"))
                .andExpect(jsonPath("$.licensePlate").value("ABC-9999"))
                .andExpect(jsonPath("$.make").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Aqua"))
                .andExpect(jsonPath("$.year").value(2023))
                .andExpect(jsonPath("$.color").value("Silver"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateAvailability_Available_Success() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);
        DriverResponse response = new DriverResponse(
                "driver-789",
                "acc-123",
                "DL-123456",
                "Colombo",
                DriverAvailability.AVAILABLE,
                6.9271,
                79.8612
        );

        when(driverService.updateAvailability(eq("driver-789"), any(UpdateAvailabilityRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateAvailability_Unavailable_Success() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.UNAVAILABLE);
        DriverResponse response = new DriverResponse(
                "driver-789",
                "acc-123",
                "DL-123456",
                "Colombo",
                DriverAvailability.UNAVAILABLE,
                6.9271,
                79.8612
        );

        when(driverService.updateAvailability(eq("driver-789"), any(UpdateAvailabilityRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateAvailability_DriverNotFound() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(DriverAvailability.AVAILABLE);

        when(driverService.updateAvailability(eq("driver-999"), any(UpdateAvailabilityRequest.class)))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: driver-999"));

        mockMvc.perform(patch("/api/drivers/driver-999/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Driver not found with id: driver-999"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateAvailability_InvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateAvailability_MissingAvailability() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_Success() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);
        DriverResponse response = new DriverResponse(
                "driver-789",
                "acc-123",
                "DL-123456",
                "Colombo",
                DriverAvailability.AVAILABLE,
                6.9271,
                79.8612
        );

        when(driverService.updateLocation(eq("driver-789"), any(UpdateLocationRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-789"))
                .andExpect(jsonPath("$.latitude").value(6.9271))
                .andExpect(jsonPath("$.longitude").value(79.8612));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_DriverNotFound() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 79.8612);

        when(driverService.updateLocation(eq("driver-999"), any(UpdateLocationRequest.class)))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: driver-999"));

        mockMvc.perform(patch("/api/drivers/driver-999/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Driver not found with id: driver-999"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_InvalidLatitude_Above90() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(91.0, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_InvalidLatitude_BelowMinus90() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(-91.0, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_InvalidLongitude_Above180() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 181.0);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_InvalidLongitude_BelowMinus180() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, -181.0);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_MissingFields() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_MissingLatitude() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longitude\": 79.8612}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_MissingLongitude() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 6.9271}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_BoundaryValues_Success() throws Exception {
        UpdateLocationRequest minBound = new UpdateLocationRequest(-90.0, -180.0);
        DriverResponse minResponse = new DriverResponse(
                "driver-789", "acc-123", "DL-123456", "Colombo",
                DriverAvailability.AVAILABLE, -90.0, -180.0
        );

        when(driverService.updateLocation(eq("driver-789"), any(UpdateLocationRequest.class))).thenReturn(minResponse);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minBound)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-90.0))
                .andExpect(jsonPath("$.longitude").value(-180.0));

        UpdateLocationRequest maxBound = new UpdateLocationRequest(90.0, 180.0);
        DriverResponse maxResponse = new DriverResponse(
                "driver-789", "acc-123", "DL-123456", "Colombo",
                DriverAvailability.AVAILABLE, 90.0, 180.0
        );

        when(driverService.updateLocation(eq("driver-789"), any(UpdateLocationRequest.class))).thenReturn(maxResponse);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxBound)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(90.0))
                .andExpect(jsonPath("$.longitude").value(180.0));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testUpdateLocation_MalformedJson() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": \"invalid\", \"longitude\": 79.8612}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetAvailableDrivers_WithServiceArea_Success() throws Exception {
        DriverResponse driver1 = new DriverResponse(
                "driver-01", "acc-1", "DL-111", "Colombo",
                DriverAvailability.AVAILABLE, 6.9271, 79.8612
        );
        DriverResponse driver2 = new DriverResponse(
                "driver-02", "acc-2", "DL-222", "Colombo",
                DriverAvailability.AVAILABLE, 6.9300, 79.8650
        );

        when(driverService.getAvailableDrivers("Colombo")).thenReturn(List.of(driver1, driver2));

        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].driverId").value("driver-01"))
                .andExpect(jsonPath("$[0].accountId").value("acc-1"))
                .andExpect(jsonPath("$[0].licenseNumber").value("DL-111"))
                .andExpect(jsonPath("$[0].serviceArea").value("Colombo"))
                .andExpect(jsonPath("$[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].latitude").value(6.9271))
                .andExpect(jsonPath("$[0].longitude").value(79.8612))
                .andExpect(jsonPath("$[1].driverId").value("driver-02"))
                .andExpect(jsonPath("$[1].serviceArea").value("Colombo"))
                .andExpect(jsonPath("$[1].availability").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetAvailableDrivers_WithoutServiceArea_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/drivers/available"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetAvailableDrivers_EmptyList() throws Exception {
        when(driverService.getAvailableDrivers("Galle")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Galle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "DRIVER")
    void testGetAvailableDrivers_DeterministicOrder() throws Exception {
        DriverResponse driver1 = new DriverResponse(
                "driver-01", "acc-1", "DL-111", "Colombo",
                DriverAvailability.AVAILABLE, 6.9271, 79.8612
        );
        DriverResponse driver2 = new DriverResponse(
                "driver-02", "acc-2", "DL-222", "Colombo",
                DriverAvailability.AVAILABLE, 6.9300, 79.8650
        );
        DriverResponse driver3 = new DriverResponse(
                "driver-03", "acc-3", "DL-333", "Colombo",
                DriverAvailability.AVAILABLE, 6.9350, 79.8700
        );

        when(driverService.getAvailableDrivers("Colombo")).thenReturn(List.of(driver1, driver2, driver3));

        mockMvc.perform(get("/api/drivers/available")
                        .param("serviceArea", "Colombo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].driverId").value("driver-01"))
                .andExpect(jsonPath("$[1].driverId").value("driver-02"))
                .andExpect(jsonPath("$[2].driverId").value("driver-03"));
    }
}
