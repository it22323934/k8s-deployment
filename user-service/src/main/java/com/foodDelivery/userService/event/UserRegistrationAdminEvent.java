package com.foodDelivery.userService.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationAdminEvent {
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String eventType;
    private String phoneNumber;
    private String confirmationUrl;
    private long timestamp;
}
