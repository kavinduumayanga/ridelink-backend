package com.ridelink.ride.config;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/ridelink_openapi_test",
        "security.jwt.secret=ride-openapi-test-secret-32-bytes"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideRepository rideRepository;

    @MockitoBean
    private DriverServiceClient driverServiceClient;

    @MockitoBean
    private FareServiceClient fareServiceClient;

    @Test
    void openApiDocumentsBearerAuthenticationAndEveryRideEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$['paths']['/api/rides']['post']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/passenger/{passengerId}']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}/assign']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}/accept']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}/start']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}/complete']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides/{rideId}/cancel']['patch']").exists())
                .andExpect(jsonPath("$['paths']['/api/rides']['post']['security'][0]['bearerAuth']").exists());
    }
}
