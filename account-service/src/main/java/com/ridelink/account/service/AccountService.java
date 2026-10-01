package com.ridelink.account.service;

import com.ridelink.account.domain.AccountStatus;
import com.ridelink.account.domain.Role;
import com.ridelink.account.domain.User;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.DuplicateEmailException;
import com.ridelink.account.exception.ValidationException;
import com.ridelink.account.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        if (request.getRole() == null || (request.getRole() != Role.PASSENGER && request.getRole() != Role.DRIVER)) {
            throw new ValidationException("Only PASSENGER and DRIVER registrations are allowed");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Email is already registered: " + request.getEmail());
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(
                null,
                request.getFirstName().trim(),
                request.getLastName().trim(),
                email,
                passwordHash,
                request.getPhone().trim(),
                request.getRole(),
                AccountStatus.ACTIVE
        );

        User savedUser = userRepository.save(user);

        return UserResponse.fromEntity(savedUser);
    }
}
