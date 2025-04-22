package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.config.JwtUtils;
import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.serviceInterfaces.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private static final String AUTHENTICATION_SERVICE = "authenticationService";

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.getUserProfile(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileRequest profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            boolean updated = userService.updateUserProfile(username, profileRequest);
            return updated
                    ? ResponseEntity.ok(new MessageResponse("Profile updated successfully"))
                    : ResponseEntity.badRequest().body(new MessageResponse("Failed to update profile"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest passwordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.changePassword(username, passwordRequest)
                ? ResponseEntity.ok(new MessageResponse("Password changed successfully"))
                : ResponseEntity.badRequest().body(new MessageResponse("Current password is incorrect"));
    }

    @PostMapping("/signout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("User logout requested");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                log.info("User {} successfully logged out", username);
            }
        }

        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateUserRole(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestParam String role) {

        log.info("Validating role {} for userId: {}, userName: {}", role, userId, userName);

        try {
            boolean isValid;
            if (userId != null) {
                isValid = userService.validateUserRoleAndEnabled(userId, role);
            } else if (userName != null) {
                isValid = userService.validateUserRoleAndEnabledByUsername(userName, role);
            } else {
                return ResponseEntity.badRequest().body(false);
            }

            log.info("Validation result: {}", isValid);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            log.error("Error validating user role: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable String userId) {
        log.info("Fetching user with ID: {}", userId);
        try {
            UserProfileResponse user = userService.getUserById(Long.valueOf(userId));
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID specified: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrieving user by ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "adminCreateUserFallback")
    public ResponseEntity<?> adminCreateUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            // Validate required fields
            if (signUpRequest.getUsername() == null || signUpRequest.getEmail() == null || signUpRequest.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Username, email and password are required!"));
            }

            UserProfileResponse createdUser = userService.createUserByAdmin(signUpRequest);
            return ResponseEntity.ok(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/update-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUpdateUser(@PathVariable Long userId, @Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            UserProfileResponse updatedUser = userService.updateUserByAdmin(userId, updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Failed to update user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> adminCreateUserFallback(SignupRequest signUpRequest, Exception e) {
        log.error("Admin user creation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse("User creation service is currently unavailable. Please try again later."));
    }

    @GetMapping("/by-role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RESTAURANT_ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getUsersByRole(@RequestParam String roleName) {
        log.info("Fetching users with role: {}", roleName);
        try {
            List<UserProfileResponse> users = userService.getUsersByRole(roleName);
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role specified: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrieving users by role: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/getUserId")
    public ResponseEntity<Long> getUserId(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        try {
            String jwt = token.substring(7);
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            log.info("Getting user ID for username: {}", username);

            // Find user by username using the userService
            return userService.findIdByUsername(username)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(null));
        } catch (Exception e) {
            log.error("Error retrieving user ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}