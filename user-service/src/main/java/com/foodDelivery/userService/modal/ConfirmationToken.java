package com.foodDelivery.userService.modal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class ConfirmationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime expiryDate;

    public ConfirmationToken(User user) {
        this.token = UUID.randomUUID().toString();
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    public ConfirmationToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}