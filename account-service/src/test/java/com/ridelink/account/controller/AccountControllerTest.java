package com.ridelink.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.GlobalExceptionHandler;
import com.ridelink.account.service.AccountService;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 201 Created and UserResponse for valid passenger registration")
    void testRegisterPassengerSuccess201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        UserResponse response = new UserResponse(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );

        when(accountService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is("665f1a2b3c4d5e6f7a8b9c0d")))
                .andExpect(jsonPath("$.firstName", is("Kavindu")))
                .andExpect(jsonPath("$.lastName", is("Umayanga")))
                .andExpect(jsonPath("$.email", is("kavindu@example.com")))
                .andExpect(jsonPath("$.phone", is("+94771234567")))
                .andExpect(jsonPath("$.role", is("PASSENGER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 201 Created for valid driver registration")
    void testRegisterDriverSuccess201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Sunil",
                "Perera",
                "sunil@example.com",
                "+94779876543",
                "driverSecureP@ss2",
                Role.DRIVER
        );

        UserResponse response = new UserResponse(
                "775a1b2c3d4e5f6a7b8c9d0e",
                "Sunil",
                "Perera",
                "sunil@example.com",
                "+94779876543",
                Role.DRIVER,
                AccountStatus.ACTIVE
        );

        when(accountService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is("775a1b2c3d4e5f6a7b8c9d0e")))
                .andExpect(jsonPath("$.role", is("DRIVER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 400 Bad Request on blank required fields")
    void testRegisterBlankFields400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "",
                "",
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path", is("/api/accounts/register")));
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 400 Bad Request on invalid email format")
    void testRegisterInvalidEmail400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "not-a-valid-email",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("Invalid email format")))
                .andExpect(jsonPath("$.path", is("/api/accounts/register")));
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 400 Bad Request on password shorter than 8 characters")
    void testRegisterShortPassword400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "short",
                Role.PASSENGER
        );

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("Password must be at least 8 characters")));
    }

    @Test
    @DisplayName("POST /api/accounts/register - Should return 409 Conflict when email already exists")
    void testRegisterDuplicateEmailConflict409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "duplicate@example.com",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        when(accountService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("Email is already registered: duplicate@example.com"));

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("CONFLICT")))
                .andExpect(jsonPath("$.message", is("Email is already registered: duplicate@example.com")))
                .andExpect(jsonPath("$.path", is("/api/accounts/register")));
    }
}
