package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareRequest;
import com.ridelink.ride.dto.FareResponse;
import com.ridelink.ride.exception.FareServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * REST client for communicating with Fare & Payment Service.
 *
 * Calls:
 * - POST /api/fares/estimate (API_CONTRACTS.md §4.1, §6.2)
 * - POST /api/fares/final   (API_CONTRACTS.md §4.2, §6.3)
 *
 * The Fare Service base URL is configurable via the
 * FARE_PAYMENT_SERVICE_URL environment variable (default: http://localhost:8084).
 *
 * All communication failures (connection errors, timeouts, 5xx responses)
 * are wrapped in FareServiceException so low-level HTTP details are
 * never exposed to callers.
 */
@Component
public class FareServiceClient {

    private final RestClient restClient;

    public FareServiceClient(@Value("${fare-payment-service.url}") String fareServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(fareServiceUrl)
                .build();
    }

    /**
     * Requests a fare estimate from Fare Service.
     * Called during ride creation (POST /api/rides).
     *
     * @param request contains rideId and distanceKm
     * @return FareResponse with totalFare to store as estimatedFare
     * @throws FareServiceException if Fare Service is unreachable or returns a server error
     */
    public FareResponse getFareEstimate(FareRequest request) {
        return callFareEndpoint("/api/fares/estimate", request);
    }

    /**
     * Requests a final fare calculation from Fare Service.
     * Called during ride completion (PATCH /api/rides/{rideId}/complete).
     *
     * @param request contains rideId and distanceKm
     * @return FareResponse with totalFare to store as finalFare
     * @throws FareServiceException if Fare Service is unreachable or returns a server error
     */
    public FareResponse getFinalFare(FareRequest request) {
        return callFareEndpoint("/api/fares/final", request);
    }

    /**
     * Shared method for calling fare endpoints.
     * Both estimate and final use the same request/response shape.
     */
    private FareResponse callFareEndpoint(String path, FareRequest request) {
        try {
            FareResponse response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new FareServiceException(
                                "Fare Service returned error: " + res.getStatusCode());
                    })
                    .body(FareResponse.class);

            if (response == null) {
                throw new FareServiceException("Fare Service returned empty response");
            }

            return response;
        } catch (FareServiceException ex) {
            // Re-throw our own exception type as-is
            throw ex;
        } catch (RestClientException ex) {
            throw new FareServiceException(
                    "Failed to communicate with Fare Service", ex);
        }
    }
}
