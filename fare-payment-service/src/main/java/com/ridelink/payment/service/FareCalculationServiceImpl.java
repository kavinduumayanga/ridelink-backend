package com.ridelink.payment.service;

import com.ridelink.payment.config.FareProperties;
import com.ridelink.payment.domain.Fare;
import com.ridelink.payment.domain.FareType;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.repository.FareRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class FareCalculationServiceImpl implements FareCalculationService {

    private final FareRepository fareRepository;
    private final FareProperties fareProperties;

    public FareCalculationServiceImpl(FareRepository fareRepository, FareProperties fareProperties) {
        this.fareRepository = fareRepository;
        this.fareProperties = fareProperties;
    }

    @Override
    public FareResponse estimateFare(FareEstimateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getDistanceKm() == null || request.getDistanceKm() <= 0) {
            throw new IllegalArgumentException("distanceKm must be greater than 0");
        }

        BigDecimal baseFare = BigDecimal.valueOf(fareProperties.getBaseFare()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratePerKm = BigDecimal.valueOf(fareProperties.getRatePerKm()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFare = calculateTotalFare(request.getDistanceKm());

        Fare fare = new Fare();
        fare.setRideId(request.getRideId());
        fare.setFareType(FareType.ESTIMATE);
        fare.setDistanceKm(request.getDistanceKm());
        fare.setBaseFare(baseFare.doubleValue());
        fare.setRatePerKm(ratePerKm.doubleValue());
        fare.setTotalFare(totalFare.doubleValue());
        fare.setCreatedAt(Instant.now());

        Fare savedFare = fareRepository.save(fare);
        return FareResponse.fromEntity(savedFare);
    }

    @Override
    public BigDecimal calculateTotalFare(double distanceKm) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("distanceKm must be greater than 0");
        }
        BigDecimal baseFare = BigDecimal.valueOf(fareProperties.getBaseFare()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratePerKm = BigDecimal.valueOf(fareProperties.getRatePerKm()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal distance = BigDecimal.valueOf(distanceKm);

        return baseFare.add(distance.multiply(ratePerKm)).setScale(2, RoundingMode.HALF_UP);
    }
}
