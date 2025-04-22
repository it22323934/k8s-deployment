package com.foodDelivery.userService.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String email;
    private String name;
    private String googlePhotoURL;
}