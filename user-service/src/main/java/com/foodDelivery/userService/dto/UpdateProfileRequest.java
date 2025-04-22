package com.foodDelivery.userService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
    private String username;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, number and special character")
    private String password;

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phoneNumber;

    @Size(max = 512, message = "Profile picture URL cannot exceed 512 characters")
    private String profilePicture;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 50, message = "Identification number cannot exceed 50 characters")
    private String identificationNumber;

    @Size(max = 20, message = "Vehicle number cannot exceed 20 characters")
    private String vehicleNumber;

    private Boolean verified;
    private Boolean disabled;

    @Valid
    private LocationDTO location;

    @Data
    public static class LocationDTO {
        @Size(max = 50, message = "Location type cannot exceed 50 characters")
        private String type;

        @Size(min = 2, max = 2, message = "Coordinates array must contain exactly 2 values [longitude, latitude]")
        private double[] coordinates;

        @Size(max = 255, message = "Location address cannot exceed 255 characters")
        private String address;
    }

    @Size(max = 10, message = "Cannot assign more than 10 roles")
    private Set<String> roles;
}