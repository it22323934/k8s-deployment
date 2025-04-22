package com.foodDelivery.userService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String password;
    private String username;
    private String email;
}