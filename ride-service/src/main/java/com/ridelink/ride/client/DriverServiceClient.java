package com.ridelink.ride.client;

import com.ridelink.ride.dto.AvailableDriverResponse;
import com.ridelink.ride.exception.DriverServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * REST client for communicating with Driver Service.
 *
 * Calls GET /api/drivers/available?serviceArea={serviceArea}
 * as defined in API_CONTRACTS.md §3.7 and §6.1.
 *
 * The Driver Service base URL is configurable via the
 * DRIVER_SERVICE_URL environment variable (default: http://localhost:8082).
 *
 * All communication failures (connection errors, timeouts, 5xx responses)
 * are wrapped in DriverServiceException so low-level HTTP details are
 * never exposed to callers.
 */
@Component
public class DriverServiceClient {

    private final RestClient restClient;

    public DriverServiceClient(@Value("${driver-service.url}") String driverServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(driverServiceUrl)
                .build();
    }

    /**
     * Queries Driver Service for all available drivers in the given service area.
     *
     * @param serviceArea the service area to filter by (e.g., "Colombo")
     * @return list of available driver DTOs (may be empty)
     * @throws DriverServiceException if Driver Service is unreachable or returns a server error
     */
    public List<AvailableDriverResponse> getAvailableDrivers(String serviceArea) {
        try {
            List<AvailableDriverResponse> drivers = restClient.get()
                    .uri("/api/drivers/available?serviceArea={serviceArea}", serviceArea)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new DriverServiceException(
                                "Driver Service returned error: " + response.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<AvailableDriverResponse>>() {});

            return drivers != null ? drivers : List.of();
        } catch (DriverServiceException ex) {
            // Re-throw our own exception type as-is
            throw ex;
        } catch (RestClientException ex) {
            throw new DriverServiceException(
                    "Failed to communicate with Driver Service", ex);
        }
    }
}
