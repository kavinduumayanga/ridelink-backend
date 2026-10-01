package com.ridelink.payment.service;

import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareFinalRequest;
import com.ridelink.payment.dto.FareResponse;

import java.math.BigDecimal;

public interface FareCalculationService {

    FareResponse estimateFare(FareEstimateRequest request);

    FareResponse calculateFinalFare(FareFinalRequest request);

    BigDecimal calculateTotalFare(double distanceKm);
}
