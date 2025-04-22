package com.foodDelivery.userService.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;
    private String address;
    private String locationType;
    private Double latitude;
    private Double longitude;
    private boolean enabled;
    private boolean isDisabled;
    private boolean isDeleted;
    private boolean isVerified;
    private String identificationNumber;
    private String vehicleNumber;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private java.util.List<String> roles;
}