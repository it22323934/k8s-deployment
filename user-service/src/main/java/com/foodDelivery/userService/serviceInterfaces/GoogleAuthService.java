package com.foodDelivery.userService.serviceInterfaces;

import com.foodDelivery.userService.dto.GoogleAuthRequest;
import com.foodDelivery.userService.dto.JwtResponse;

public interface GoogleAuthService {
    JwtResponse processGoogleAuth(GoogleAuthRequest request);
}