package com.foodDelivery.userService.modal;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class UserNotificationEvent {
    private String email;
    private String eventType;
    private Map<String, Object> data = new HashMap<>();
    private long timestamp;
}