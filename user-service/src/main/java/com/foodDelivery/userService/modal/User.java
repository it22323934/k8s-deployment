package com.foodDelivery.userService.modal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName = "";

    @Column(name = "last_name", nullable = false)
    private String lastName = "";

    @Column(name = "phone_number")
    private String phoneNumber = "";

    @Column(name = "profile_image")
    private String profileImage = "";

    @Column(name = "address")
    private String address = "";

    @Column(name = "location_type")
    private String locationType = "Point";

    @Column(name = "latitude")
    private Double latitude = 0.0;

    @Column(name = "longitude")
    private Double longitude = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private boolean enabled = false;

    @Column(name = "is_disabled")
    private boolean isDisabled = false;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @Column(name = "identification_number",unique = true)
    private String identificationNumber = "";

    @Column(name = "vehicle_number")
    private String vehicleNumber = "";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}