package com.ridelink.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.exception.GlobalExceptionHandler;
import com.ridelink.payment.service.FareCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FareControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FareCalculationService fareCalculationService;

    @InjectMocks
    private FareController fareController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fareController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/fares/estimate should return 200 OK with estimated fare response")
    void testEstimateFareSuccess() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest("995c3d4e5f6a7b8c9d0e1f2a", 12.5);
        FareResponse mockResponse = new FareResponse(
                "aa5d4e5f6a7b8c9d0e1f2a3b",
                "995c3d4e5f6a7b8c9d0e1f2a",
                "ESTIMATE",
                12.5,
                200.00,
                50.00,
                825.00
        );

        when(fareCalculationService.estimateFare(any(FareEstimateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareId").value("aa5d4e5f6a7b8c9d0e1f2a3b"))
                .andExpect(jsonPath("$.rideId").value("995c3d4e5f6a7b8c9d0e1f2a"))
                .andExpect(jsonPath("$.fareType").value("ESTIMATE"))
                .andExpect(jsonPath("$.distanceKm").value(12.5))
                .andExpect(jsonPath("$.baseFare").value(200.00))
                .andExpect(jsonPath("$.ratePerKm").value(50.00))
                .andExpect(jsonPath("$.totalFare").value(825.00));
    }

    @Test
    @DisplayName("POST /api/fares/estimate with zero distance should return 400 VALIDATION_ERROR")
    void testEstimateFareZeroDistance() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest("995c3d4e5f6a7b8c9d0e1f2a", 0.0);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/fares/estimate"));
    }

    @Test
    @DisplayName("POST /api/fares/estimate with negative distance should return 400 VALIDATION_ERROR")
    void testEstimateFareNegativeDistance() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest("995c3d4e5f6a7b8c9d0e1f2a", -3.5);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/fares/estimate"));
    }

    @Test
    @DisplayName("POST /api/fares/estimate with blank rideId should return 400 VALIDATION_ERROR")
    void testEstimateFareBlankRideId() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest("", 10.0);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/fares/estimate"));
    }
}
