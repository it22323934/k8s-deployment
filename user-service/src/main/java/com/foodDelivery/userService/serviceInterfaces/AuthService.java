package com.foodDelivery.userService.serviceInterfaces;

import com.foodDelivery.userService.dto.JwtResponse;
import com.foodDelivery.userService.dto.LoginRequest;
import com.foodDelivery.userService.dto.PasswordResetRequest;
import com.foodDelivery.userService.dto.SignupRequest;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> authenticateUser(LoginRequest loginRequest);
    ResponseEntity<?> registerUser(SignupRequest signUpRequest);
    ResponseEntity<?> confirmUserAccount(String confirmationToken);
    ResponseEntity<?> forgotPassword(String email);
    ResponseEntity<?> validateResetToken(String token);
    ResponseEntity<?> resetPassword(PasswordResetRequest resetRequest);
}