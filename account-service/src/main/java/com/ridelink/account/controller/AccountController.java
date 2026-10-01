package com.ridelink.account.controller;

import com.ridelink.account.dto.ErrorResponse;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ForbiddenException;
import com.ridelink.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account", description = "Account management and registration endpoints")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user", description = "Public endpoint to create a new PASSENGER or DRIVER account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user profile",
            description = "Retrieves account profile. Accessible by account owner (PASSENGER/DRIVER) or ADMIN.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another user's profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getProfile(
            @Parameter(description = "User ID (MongoDB ObjectId)", required = true)
            @PathVariable("userId") String userId) {
        checkProfileAccess(userId);
        UserResponse response = accountService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @Operation(
            summary = "Update user profile",
            description = "Updates profile fields (firstName, lastName, phone). Accessible by account owner (PASSENGER/DRIVER) or ADMIN.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot update another user's profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateProfile(
            @Parameter(description = "User ID (MongoDB ObjectId)", required = true)
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        checkProfileAccess(userId);
        UserResponse response = accountService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/status")
    @Operation(
            summary = "Update account status",
            description = "Updates account status (ACTIVE or INACTIVE). Restricted to ADMIN role only.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account status updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateStatus(
            @Parameter(description = "User ID (MongoDB ObjectId)", required = true)
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        UserResponse response = accountService.updateStatus(userId, request);
        return ResponseEntity.ok(response);
    }

    private void checkProfileAccess(String targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ForbiddenException("Access denied: unauthenticated");
        }
        String authenticatedUserId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        if (!authenticatedUserId.equals(targetUserId) && !isAdmin) {
            throw new ForbiddenException("Access denied: you can only access your own profile");
        }
    }
}
