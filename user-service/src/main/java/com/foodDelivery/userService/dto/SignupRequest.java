package com.foodDelivery.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "Username is required")
    private String username;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
    private String firstName;
    private String lastName;
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    private String profilePicture;
    private String address;
    private String identificationNumber;
    private String vehicleNumber;
    private boolean verified;
    private boolean disabled;
    private LocationDTO location;
    @Data
    public static class LocationDTO {
        private String type;
        private double[] coordinates;
        private String address;
    }
    private Set<String> roles;
}