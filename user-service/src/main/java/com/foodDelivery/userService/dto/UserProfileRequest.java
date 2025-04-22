package com.foodDelivery.userService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserProfileRequest {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phoneNumber;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 512, message = "Profile picture URL cannot exceed 512 characters")
    private String profilePicture;

    @Valid
    private LocationDTO location;

    @Data
    public static class LocationDTO {
        private String type;

        @Size(min = 2, max = 2, message = "Coordinates array must contain exactly 2 values [longitude, latitude]")
        private double[] coordinates;

        @Size(max = 255, message = "Address cannot exceed 255 characters")
        private String address;
    }
}