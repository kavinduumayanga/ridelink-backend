package com.ridelink.account.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.service.AccountService;
import com.ridelink.account.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AuthService authService;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String createExpiredToken(String userId, Role role) {
        Date now = new Date(System.currentTimeMillis() - 200000);
        Date expiry = new Date(System.currentTimeMillis() - 100000);
        return Jwts.builder()
                .subject(userId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ==========================================
    // 1. PUBLIC ENDPOINTS (No JWT required)
    // ==========================================

    @Test
    @DisplayName("Public - Registration endpoint is accessible without JWT")
    void testRegisterAccessibleWithoutJwt() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Nethmi",
                "Perera",
                "nethmi@example.com",
                "+94771112233",
                "secureP@ss1",
                Role.PASSENGER
        );

        UserResponse response = new UserResponse(
                "user-100",
                "Nethmi",
                "Perera",
                "nethmi@example.com",
                "+94771112233",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );

        when(accountService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is("user-100")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Public - Login endpoint is accessible without JWT")
    void testLoginAccessibleWithoutJwt() throws Exception {
        LoginRequest request = new LoginRequest("nethmi@example.com", "secureP@ss1");
        LoginResponse response = new LoginResponse("user-100", "nethmi@example.com", Role.PASSENGER, "sample.jwt.token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("user-100")))
                .andExpect(jsonPath("$.token", is("sample.jwt.token")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Public - OpenAPI documentation is accessible without JWT")
    void testOpenApiDocsAccessibleWithoutJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. AUTHENTICATION (401 Unauthorized)
    // ==========================================

    @Test
    @DisplayName("Authentication - Protected endpoint without JWT returns 401 UNAUTHENTICATED")
    void testProtectedEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/accounts/user-100"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.path", is("/api/accounts/user-100")));
    }

    @Test
    @DisplayName("Authentication - Protected endpoint with invalid JWT returns 401 UNAUTHENTICATED")
    void testProtectedEndpointWithInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/accounts/user-100")
                        .header("Authorization", "Bearer invalid.jwt.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.path", is("/api/accounts/user-100")));
    }

    @Test
    @DisplayName("Authentication - Protected endpoint with expired JWT returns 401 UNAUTHENTICATED")
    void testProtectedEndpointWithExpiredJwtReturns401() throws Exception {
        String expiredToken = createExpiredToken("user-100", Role.PASSENGER);

        mockMvc.perform(get("/api/accounts/user-100")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHENTICATED")))
                .andExpect(jsonPath("$.path", is("/api/accounts/user-100")));
    }

    // ==========================================
    // 3. PASSENGER ROLE AUTHORIZATION
    // ==========================================

    @Test
    @DisplayName("PASSENGER - Can retrieve own profile")
    void testPassengerCanRetrieveOwnProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("passenger-1", Role.PASSENGER);
        UserResponse response = new UserResponse(
                "passenger-1", "Alice", "Smith", "alice@example.com",
                "+94771112233", Role.PASSENGER, AccountStatus.ACTIVE
        );
        when(accountService.getProfile("passenger-1")).thenReturn(response);

        mockMvc.perform(get("/api/accounts/passenger-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("passenger-1")))
                .andExpect(jsonPath("$.role", is("PASSENGER")));
    }

    @Test
    @DisplayName("PASSENGER - Can update own profile")
    void testPassengerCanUpdateOwnProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("passenger-1", Role.PASSENGER);
        UpdateProfileRequest request = new UpdateProfileRequest("AliceUpdated", "SmithUpdated", "+94779998877");
        UserResponse response = new UserResponse(
                "passenger-1", "AliceUpdated", "SmithUpdated", "alice@example.com",
                "+94779998877", Role.PASSENGER, AccountStatus.ACTIVE
        );
        when(accountService.updateProfile(eq("passenger-1"), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/passenger-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("passenger-1")))
                .andExpect(jsonPath("$.firstName", is("AliceUpdated")));
    }

    @Test
    @DisplayName("PASSENGER - Cannot retrieve another user's profile returns 403 FORBIDDEN")
    void testPassengerCannotRetrieveOtherProfileReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("passenger-1", Role.PASSENGER);

        mockMvc.perform(get("/api/accounts/other-user-999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("PASSENGER - Cannot update another user's profile returns 403 FORBIDDEN")
    void testPassengerCannotUpdateOtherProfileReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("passenger-1", Role.PASSENGER);
        UpdateProfileRequest request = new UpdateProfileRequest("Hacked", "User", "+94770000000");

        mockMvc.perform(put("/api/accounts/other-user-999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("PASSENGER - Cannot perform status management returns 403 FORBIDDEN")
    void testPassengerCannotUpdateAccountStatusReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("passenger-1", Role.PASSENGER);
        UpdateStatusRequest request = new UpdateStatusRequest(AccountStatus.INACTIVE);

        mockMvc.perform(patch("/api/accounts/passenger-1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // ==========================================
    // 4. DRIVER ROLE AUTHORIZATION
    // ==========================================

    @Test
    @DisplayName("DRIVER - Can retrieve own profile")
    void testDriverCanRetrieveOwnProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("driver-1", Role.DRIVER);
        UserResponse response = new UserResponse(
                "driver-1", "Bob", "Jones", "bob@example.com",
                "+94772223344", Role.DRIVER, AccountStatus.ACTIVE
        );
        when(accountService.getProfile("driver-1")).thenReturn(response);

        mockMvc.perform(get("/api/accounts/driver-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("driver-1")))
                .andExpect(jsonPath("$.role", is("DRIVER")));
    }

    @Test
    @DisplayName("DRIVER - Can update own profile")
    void testDriverCanUpdateOwnProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("driver-1", Role.DRIVER);
        UpdateProfileRequest request = new UpdateProfileRequest("BobUpdated", "JonesUpdated", "+94778887766");
        UserResponse response = new UserResponse(
                "driver-1", "BobUpdated", "JonesUpdated", "bob@example.com",
                "+94778887766", Role.DRIVER, AccountStatus.ACTIVE
        );
        when(accountService.updateProfile(eq("driver-1"), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/driver-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("driver-1")))
                .andExpect(jsonPath("$.firstName", is("BobUpdated")));
    }

    @Test
    @DisplayName("DRIVER - Cannot retrieve another user's profile returns 403 FORBIDDEN")
    void testDriverCannotRetrieveOtherProfileReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("driver-1", Role.DRIVER);

        mockMvc.perform(get("/api/accounts/other-user-888")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("DRIVER - Cannot perform status management returns 403 FORBIDDEN")
    void testDriverCannotUpdateAccountStatusReturns403() throws Exception {
        String token = jwtTokenProvider.generateToken("driver-1", Role.DRIVER);
        UpdateStatusRequest request = new UpdateStatusRequest(AccountStatus.INACTIVE);

        mockMvc.perform(patch("/api/accounts/driver-1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // ==========================================
    // 5. ADMIN ROLE AUTHORIZATION
    // ==========================================

    @Test
    @DisplayName("ADMIN - Can update account status successfully")
    void testAdminCanUpdateAccountStatus() throws Exception {
        String token = jwtTokenProvider.generateToken("admin-1", Role.ADMIN);
        UpdateStatusRequest request = new UpdateStatusRequest(AccountStatus.INACTIVE);
        UserResponse response = new UserResponse(
                "target-user-1", "Target", "User", "target@example.com",
                "+94773334455", Role.PASSENGER, AccountStatus.INACTIVE
        );
        when(accountService.updateStatus(eq("target-user-1"), any(UpdateStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/accounts/target-user-1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("target-user-1")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    @DisplayName("ADMIN - Can retrieve any user's profile")
    void testAdminCanRetrieveAnyUserProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("admin-1", Role.ADMIN);
        UserResponse response = new UserResponse(
                "target-user-1", "Target", "User", "target@example.com",
                "+94773334455", Role.PASSENGER, AccountStatus.ACTIVE
        );
        when(accountService.getProfile("target-user-1")).thenReturn(response);

        mockMvc.perform(get("/api/accounts/target-user-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("target-user-1")));
    }

    @Test
    @DisplayName("ADMIN - Can update any user's profile")
    void testAdminCanUpdateAnyUserProfile() throws Exception {
        String token = jwtTokenProvider.generateToken("admin-1", Role.ADMIN);
        UpdateProfileRequest request = new UpdateProfileRequest("AdminEditedFirst", "AdminEditedLast", "+94770001122");
        UserResponse response = new UserResponse(
                "target-user-1", "AdminEditedFirst", "AdminEditedLast", "target@example.com",
                "+94770001122", Role.PASSENGER, AccountStatus.ACTIVE
        );
        when(accountService.updateProfile(eq("target-user-1"), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/accounts/target-user-1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("target-user-1")))
                .andExpect(jsonPath("$.firstName", is("AdminEditedFirst")));
    }
}
