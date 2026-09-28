package com.ridelink.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fare")
public class FareProperties {

    private double baseFare = 200.00;
    private double ratePerKm = 50.00;

    public FareProperties() {
    }

    public FareProperties(double baseFare, double ratePerKm) {
        this.baseFare = baseFare;
        this.ratePerKm = ratePerKm;
    }

    public double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    public double getRatePerKm() {
        return ratePerKm;
    }

    public void setRatePerKm(double ratePerKm) {
        this.ratePerKm = ratePerKm;
    }
}
