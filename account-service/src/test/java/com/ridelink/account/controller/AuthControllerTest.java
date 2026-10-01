package com.ridelink.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.account.domain.Role;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.exception.AccountInactiveException;
import com.ridelink.account.exception.GlobalExceptionHandler;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.service.AuthService;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/accounts/login - Should return 200 OK and LoginResponse for valid credentials")
    void testLoginSuccess200() throws Exception {
        LoginRequest request = new LoginRequest("kavindu@example.com", "secureP@ss1");
        LoginResponse response = new LoginResponse(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "kavindu@example.com",
                Role.PASSENGER,
                "mock.jwt.token"
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("665f1a2b3c4d5e6f7a8b9c0d")))
                .andExpect(jsonPath("$.email", is("kavindu@example.com")))
                .andExpect(jsonPath("$.role", is("PASSENGER")))
                .andExpect(jsonPath("$.token", is("mock.jwt.token")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/accounts/login - Should return 401 Unauthorized for invalid credentials")
    void testLoginInvalidCredentials401() throws Exception {
        LoginRequest request = new LoginRequest("kavindu@example.com", "wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")))
                .andExpect(jsonPath("$.path", is("/api/accounts/login")));
    }

    @Test
    @DisplayName("POST /api/accounts/login - Should return 403 Forbidden for inactive account")
    void testLoginInactiveAccount403() throws Exception {
        LoginRequest request = new LoginRequest("inactive@example.com", "secureP@ss1");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccountInactiveException("Account is inactive"));

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", is("Account is inactive")))
                .andExpect(jsonPath("$.path", is("/api/accounts/login")));
    }

    @Test
    @DisplayName("POST /api/accounts/login - Should return 400 Bad Request on blank credentials")
    void testLoginBlankCredentials400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/accounts/login")));
    }
}
