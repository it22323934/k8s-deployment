package com.foodDelivery.userService.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationEvent {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String eventType;
    private String phoneNumber;
    private String confirmationUrl;
    private long timestamp;
}