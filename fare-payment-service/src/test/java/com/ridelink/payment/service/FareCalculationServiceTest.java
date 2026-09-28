package com.ridelink.payment.service;

import com.ridelink.payment.config.FareProperties;
import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.FareType;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.repository.FareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareCalculationServiceTest {

    @Mock
    private FareRepository fareRepository;

    private FareProperties fareProperties;
    private FareCalculationServiceImpl fareCalculationService;

    @BeforeEach
    void setUp() {
        fareProperties = new FareProperties(200.00, 50.00);
        fareCalculationService = new FareCalculationServiceImpl(fareRepository, fareProperties);
    }

    @Test
    @DisplayName("Should correctly calculate normal fare matching API contracts example")
    void testNormalFareCalculation() {
        // baseFare = 200.0, ratePerKm = 50.0, distance = 12.5 -> 200 + (12.5 * 50) = 825.00
        BigDecimal totalFare = fareCalculationService.calculateTotalFare(12.5);
        assertEquals(new BigDecimal("825.00"), totalFare);
    }

    @Test
    @DisplayName("Should correctly calculate fare with decimal distances and proper rounding")
    void testDecimalDistanceFareCalculation() {
        // distance = 5.5 -> 200 + (5.5 * 50) = 475.00
        BigDecimal fare1 = fareCalculationService.calculateTotalFare(5.5);
        assertEquals(new BigDecimal("475.00"), fare1);

        // distance = 13.2 -> 200 + (13.2 * 50) = 860.00
        BigDecimal fare2 = fareCalculationService.calculateTotalFare(13.2);
        assertEquals(new BigDecimal("860.00"), fare2);

        // decimal distance requiring rounding: 3.333 * 50 = 166.65 -> 366.65
        BigDecimal fare3 = fareCalculationService.calculateTotalFare(3.333);
        assertEquals(new BigDecimal("366.65"), fare3);
    }

    @Test
    @DisplayName("Should throw exception when distance is zero or negative")
    void testInvalidZeroOrNegativeDistance() {
        IllegalArgumentException zeroEx = assertThrows(IllegalArgumentException.class, () ->
                fareCalculationService.calculateTotalFare(0.0)
        );
        assertEquals("distanceKm must be greater than 0", zeroEx.getMessage());

        IllegalArgumentException negativeEx = assertThrows(IllegalArgumentException.class, () ->
                fareCalculationService.calculateTotalFare(-5.0)
        );
        assertEquals("distanceKm must be greater than 0", negativeEx.getMessage());
    }

    @Test
    @DisplayName("Should estimate fare, persist Fare document, and return complete FareResponse")
    void testEstimateFarePersistsAndReturnsResponse() {
        FareEstimateRequest request = new FareEstimateRequest("ride-995", 12.5);

        when(fareRepository.save(any(Fare.class))).thenAnswer(invocation -> {
            Fare fare = invocation.getArgument(0);
            fare.setId("generated-fare-id-123");
            return fare;
        });

        FareResponse response = fareCalculationService.estimateFare(request);

        assertNotNull(response);
        assertEquals("generated-fare-id-123", response.getFareId());
        assertEquals("ride-995", response.getRideId());
        assertEquals("ESTIMATE", response.getFareType());
        assertEquals(12.5, response.getDistanceKm());
        assertEquals(200.00, response.getBaseFare());
        assertEquals(50.00, response.getRatePerKm());
        assertEquals(825.00, response.getTotalFare());

        ArgumentCaptor<Fare> captor = ArgumentCaptor.forClass(Fare.class);
        verify(fareRepository, times(1)).save(captor.capture());
        Fare savedEntity = captor.getValue();
        assertEquals("ride-995", savedEntity.getRideId());
        assertEquals(FareType.ESTIMATE, savedEntity.getFareType());
        assertEquals(12.5, savedEntity.getDistanceKm());
        assertEquals(200.00, savedEntity.getBaseFare());
        assertEquals(50.00, savedEntity.getRatePerKm());
        assertEquals(825.00, savedEntity.getTotalFare());
        assertNotNull(savedEntity.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception when estimateFare is called with null or invalid distance")
    void testEstimateFareNullOrInvalidDistance() {
        assertThrows(IllegalArgumentException.class, () ->
                fareCalculationService.estimateFare(null)
        );

        FareEstimateRequest nullDistanceReq = new FareEstimateRequest("ride-1", null);
        assertThrows(IllegalArgumentException.class, () ->
                fareCalculationService.estimateFare(nullDistanceReq)
        );

        FareEstimateRequest zeroDistanceReq = new FareEstimateRequest("ride-1", 0.0);
        assertThrows(IllegalArgumentException.class, () ->
                fareCalculationService.estimateFare(zeroDistanceReq)
        );
    }
}
