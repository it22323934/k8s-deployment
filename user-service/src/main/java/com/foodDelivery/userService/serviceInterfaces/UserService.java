package com.foodDelivery.userService.serviceInterfaces;

import com.foodDelivery.userService.dto.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserService {
    Optional<UserProfileResponse> getUserProfile(String username);
    UserProfileResponse getUserByUserName(String username);
    boolean updateUserProfile(String username, UserProfileRequest profileRequest);
    boolean changePassword(String username, PasswordChangeRequest request);
    List<UserProfileResponse> getAllUsers();
    UserProfileResponse createUserByAdmin(SignupRequest signUpRequest);
    UserProfileResponse updateUserByAdmin(Long userId, UpdateProfileRequest updateRequest);
    boolean requestPasswordReset(String email);
    boolean resetPassword(PasswordResetRequest resetRequest);
    boolean validateUserRole(Long userId, String role);
    boolean validateUserRoleAndEnabledByUsername(String username, String role);
    boolean validateUserRoleAndEnabled(Long userId, String role);
    List<UserProfileResponse> getUsersByRole(String roleName);
    UserProfileResponse getUserById(Long userId);
    Optional<Long> findIdByUsername(String username);
}