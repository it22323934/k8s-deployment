package com.foodDelivery.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequest {
    @NotBlank
    private String token;
    @NotBlank
    @Size(min = 6, max = 40)
    private String newPassword;
}