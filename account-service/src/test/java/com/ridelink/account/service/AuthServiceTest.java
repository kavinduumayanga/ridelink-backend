package com.ridelink.account.service;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.LoginResponse;
import com.ridelink.account.exception.AccountInactiveException;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User activePassenger;
    private User activeDriver;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activePassenger = new User(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "$2a$10$encodedPasswordHash",
                "+94771234567",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );

        activeDriver = new User(
                "775a1b2c3d4e5f6a7b8c9d0e",
                "Sunil",
                "Perera",
                "sunil@example.com",
                "$2a$10$encodedDriverHash",
                "+94779876543",
                Role.DRIVER,
                AccountStatus.ACTIVE
        );

        inactiveUser = new User(
                "885f1a2b3c4d5e6f7a8b9c0d",
                "Inactive",
                "User",
                "inactive@example.com",
                "$2a$10$encodedPasswordHash",
                "+94770000000",
                Role.PASSENGER,
                AccountStatus.INACTIVE
        );
    }

    @Test
    @DisplayName("Should successfully authenticate PASSENGER and issue JWT")
    void testValidPassengerLogin() {
        LoginRequest request = new LoginRequest("kavindu@example.com", "secureP@ss1");

        when(userRepository.findByEmail("kavindu@example.com")).thenReturn(Optional.of(activePassenger));
        when(passwordEncoder.matches("secureP@ss1", activePassenger.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(activePassenger)).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("665f1a2b3c4d5e6f7a8b9c0d", response.getUserId());
        assertEquals("kavindu@example.com", response.getEmail());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals("mock.jwt.token", response.getToken());

        verify(userRepository).findByEmail("kavindu@example.com");
        verify(passwordEncoder).matches("secureP@ss1", activePassenger.getPasswordHash());
        verify(jwtTokenProvider).generateToken(activePassenger);
    }

    @Test
    @DisplayName("Should successfully authenticate DRIVER and issue JWT")
    void testValidDriverLogin() {
        LoginRequest request = new LoginRequest("sunil@example.com", "driverSecureP@ss2");

        when(userRepository.findByEmail("sunil@example.com")).thenReturn(Optional.of(activeDriver));
        when(passwordEncoder.matches("driverSecureP@ss2", activeDriver.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(activeDriver)).thenReturn("mock.driver.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("775a1b2c3d4e5f6a7b8c9d0e", response.getUserId());
        assertEquals("sunil@example.com", response.getEmail());
        assertEquals(Role.DRIVER, response.getRole());
        assertEquals("mock.driver.jwt.token", response.getToken());
    }

    @Test
    @DisplayName("Should reject unknown email with InvalidCredentialsException (401)")
    void testUnknownEmailRejected() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("Should reject incorrect password with InvalidCredentialsException (401)")
    void testIncorrectPasswordRejected() {
        LoginRequest request = new LoginRequest("kavindu@example.com", "wrongPassword");

        when(userRepository.findByEmail("kavindu@example.com")).thenReturn(Optional.of(activePassenger));
        when(passwordEncoder.matches("wrongPassword", activePassenger.getPasswordHash())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("Should reject INACTIVE user login with AccountInactiveException (403)")
    void testInactiveAccountRejected() {
        LoginRequest request = new LoginRequest("inactive@example.com", "secureP@ss1");

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("secureP@ss1", inactiveUser.getPasswordHash())).thenReturn(true);

        AccountInactiveException exception = assertThrows(
                AccountInactiveException.class,
                () -> authService.login(request)
        );

        assertEquals("Account is inactive", exception.getMessage());
        verifyNoInteractions(jwtTokenProvider);
    }
}
