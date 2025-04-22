package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.dto.GoogleAuthRequest;
import com.foodDelivery.userService.dto.JwtResponse;
import com.foodDelivery.userService.dto.MessageResponse;
import com.foodDelivery.userService.serviceInterfaces.GoogleAuthService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthController {

    private static final String AUTHENTICATION_SERVICE = "authenticationService";
    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "googleAuthFallback")
    public ResponseEntity<?> authenticateGoogle(@RequestBody GoogleAuthRequest request) {
        try {
            log.info("Processing Google authentication request for: {}", request.getEmail());

            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is required"));
            }

            if (request.getName() == null || request.getName().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Name is required"));
            }

            JwtResponse jwtResponse = googleAuthService.processGoogleAuth(request);
            log.info("Google authentication successful for: {}", request.getEmail());

            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                    .body(new MessageResponse("Error: Google authentication failed. " + e.getMessage()));
        }
    }

    public ResponseEntity<?> googleAuthFallback(GoogleAuthRequest request, Exception e) {
        log.error("Circuit breaker triggered for Google authentication: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Service temporarily unavailable. Please try again later."));
    }
}