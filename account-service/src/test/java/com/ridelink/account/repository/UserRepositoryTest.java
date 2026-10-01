package com.ridelink.account.repository;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "665f1a2b3c4d5e6f7a8b9c0d",
                "Kavindu",
                "Umayanga",
                "kavindu@example.com",
                "$2a$12$hashedPasswordExample",
                "+94771234567",
                Role.PASSENGER,
                AccountStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        when(userRepository.findByEmail("kavindu@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> found = userRepository.findByEmail("kavindu@example.com");

        assertTrue(found.isPresent());
        assertEquals("kavindu@example.com", found.get().getEmail());
        assertEquals(Role.PASSENGER, found.get().getRole());
        verify(userRepository).findByEmail("kavindu@example.com");
    }

    @Test
    @DisplayName("Should check whether user exists by email")
    void testExistsByEmail() {
        when(userRepository.existsByEmail("kavindu@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        assertTrue(userRepository.existsByEmail("kavindu@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
        verify(userRepository).existsByEmail("kavindu@example.com");
        verify(userRepository).existsByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should find users by role")
    void testFindByRole() {
        when(userRepository.findByRole(Role.PASSENGER)).thenReturn(List.of(sampleUser));

        List<User> passengers = userRepository.findByRole(Role.PASSENGER);

        assertEquals(1, passengers.size());
        assertEquals(Role.PASSENGER, passengers.getFirst().getRole());
        verify(userRepository).findByRole(Role.PASSENGER);
    }

    @Test
    @DisplayName("Should find users by status")
    void testFindByStatus() {
        when(userRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(sampleUser));

        List<User> activeUsers = userRepository.findByStatus(AccountStatus.ACTIVE);

        assertEquals(1, activeUsers.size());
        assertEquals(AccountStatus.ACTIVE, activeUsers.getFirst().getStatus());
        verify(userRepository).findByStatus(AccountStatus.ACTIVE);
    }
}
