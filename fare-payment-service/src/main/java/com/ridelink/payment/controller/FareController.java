package com.ridelink.payment.controller;

import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.FareResponse;
import com.ridelink.payment.service.FareCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
public class FareController {

    private final FareCalculationService fareCalculationService;

    public FareController(FareCalculationService fareCalculationService) {
        this.fareCalculationService = fareCalculationService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<FareResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        FareResponse response = fareCalculationService.estimateFare(request);
        return ResponseEntity.ok(response);
    }
}
