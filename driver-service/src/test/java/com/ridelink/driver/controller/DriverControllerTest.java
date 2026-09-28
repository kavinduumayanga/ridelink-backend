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
import com.ridelink.driver.service.DriverService;
import com.ridelink.driver.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
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
    void testCreateDriver_ValidationError() throws Exception {
        CreateDriverRequest invalidRequest = new CreateDriverRequest("", "", "");

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
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
    void testGetDriverById_NotFound() throws Exception {
        when(driverService.getDriverById("driver-999"))
                .thenThrow(new ResourceNotFoundException("Driver not found with id: driver-999"));

        mockMvc.perform(get("/api/drivers/driver-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Driver not found with id: driver-999"));
    }

    @Test
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
    void testUpdateAvailability_InvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availability\":\"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
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
    void testUpdateLocation_InvalidLatitude_Above90() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(91.0, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testUpdateLocation_InvalidLatitude_BelowMinus90() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(-91.0, 79.8612);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testUpdateLocation_InvalidLongitude_Above180() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, 181.0);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testUpdateLocation_InvalidLongitude_BelowMinus180() throws Exception {
        UpdateLocationRequest request = new UpdateLocationRequest(6.9271, -181.0);

        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void testUpdateLocation_MissingFields() throws Exception {
        mockMvc.perform(patch("/api/drivers/driver-789/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
