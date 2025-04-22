package com.foodDelivery.userService.repository;

import com.foodDelivery.userService.modal.Role;
import com.foodDelivery.userService.modal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByIdentificationNumber(String identificationNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    @Query("SELECT u FROM User u")
    List<User> findAllUsers();
    List<User> findByRolesName(String roleName);
    List<User> findByRolesContaining(Role role);
}