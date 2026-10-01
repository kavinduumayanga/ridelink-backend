package com.ridelink.account.service;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.ValidationException;
import com.ridelink.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private RegisterRequest passengerRequest;
    private RegisterRequest driverRequest;

    @BeforeEach
    void setUp() {
        passengerRequest = new RegisterRequest(
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "+94771234567",
                "secureP@ss1",
                Role.PASSENGER
        );

        driverRequest = new RegisterRequest(
                "Sunil",
                "Perera",
                "sunil@example.com",
                "+94779876543",
                "driverSecureP@ss2",
                Role.DRIVER
        );
    }

    @Test
    @DisplayName("Should successfully register a PASSENGER account with status ACTIVE")
    void testRegisterPassengerSuccess() {
        when(userRepository.existsByEmail("kavindu@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secureP@ss1")).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("665f1a2b3c4d5e6f7a8b9c0d");
            return u;
        });

        UserResponse response = accountService.register(passengerRequest);

        assertNotNull(response);
        assertEquals("665f1a2b3c4d5e6f7a8b9c0d", response.getUserId());
        assertEquals("Kavindu", response.getFirstName());
        assertEquals("Umayanga", response.getLastName());
        assertEquals("kavindu@example.com", response.getEmail());
        assertEquals("+94771234567", response.getPhone());
        assertEquals(Role.PASSENGER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(userRepository).existsByEmail("kavindu@example.com");
        verify(passwordEncoder).encode("secureP@ss1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully register a DRIVER account with status ACTIVE")
    void testRegisterDriverSuccess() {
        when(userRepository.existsByEmail("sunil@example.com")).thenReturn(false);
        when(passwordEncoder.encode("driverSecureP@ss2")).thenReturn("$2a$10$encodedDriverHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("775a1b2c3d4e5f6a7b8c9d0e");
            return u;
        });

        UserResponse response = accountService.register(driverRequest);

        assertNotNull(response);
        assertEquals("775a1b2c3d4e5f6a7b8c9d0e", response.getUserId());
        assertEquals("Sunil", response.getFirstName());
        assertEquals("Perera", response.getLastName());
        assertEquals("sunil@example.com", response.getEmail());
        assertEquals("+94779876543", response.getPhone());
        assertEquals(Role.DRIVER, response.getRole());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Should ensure password is BCrypt encoded and plaintext password is never persisted")
    void testPasswordIsBcryptEncodedAndPlaintextNeverPersisted() {
        when(userRepository.existsByEmail("kavindu@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secureP@ss1")).thenReturn("$2a$10$superBcryptHashedValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.register(passengerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$superBcryptHashedValue", savedUser.getPasswordHash());
        assertNotEquals("secureP@ss1", savedUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should reject duplicate email with DuplicateEmailException (409)")
    void testDuplicateEmailRejected() {
        when(userRepository.existsByEmail("kavindu@example.com")).thenReturn(true);

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> accountService.register(passengerRequest)
        );

        assertEquals("Email is already registered: kavindu@example.com", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject ADMIN role registration via public registration endpoint")
    void testAdminRoleRejected() {
        RegisterRequest adminRequest = new RegisterRequest(
                "Admin",
                "User",
                "admin@example.com",
                "+94770000000",
                "adminSecurePass1",
                Role.ADMIN
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> accountService.register(adminRequest)
        );

        assertEquals("Only PASSENGER and DRIVER registrations are allowed", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject null role registration")
    void testNullRoleRejected() {
        RegisterRequest nullRoleRequest = new RegisterRequest(
                "No",
                "Role",
                "norole@example.com",
                "+94770000000",
                "password123",
                null
        );

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> accountService.register(nullRoleRequest)
        );

        assertEquals("Only PASSENGER and DRIVER registrations are allowed", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
