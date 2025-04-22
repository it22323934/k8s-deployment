package com.foodDelivery.userService.serviceInterfaces;

import com.foodDelivery.userService.event.PasswordResetEvent;
import com.foodDelivery.userService.event.UserRegistrationAdminEvent;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import java.util.Map;

public interface KafkaProducerService {
    void sendUserRegistrationEvent(UserRegistrationEvent event);
    void sendAdminUserRegistrationEvent(UserRegistrationAdminEvent event);
    void sendPasswordResetEvent(Long userId, String email, String firstName,
                                String eventType, String resetUrl);
    void sendProfileUpdateNotification(Map<String, Object> eventData);
    void sendUserNotification(String email, String eventType, Map<String, Object> data);
}