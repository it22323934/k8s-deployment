package com.foodDelivery.userService.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetEvent {
    private Long userId;
    private String email;
    private String firstName;
    private String eventType;
    private String resetUrl;
    private long timestamp;
}