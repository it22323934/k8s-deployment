package com.foodDelivery.userService.serviceImplementation;

import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.event.UserRegistrationAdminEvent;
import com.foodDelivery.userService.modal.ConfirmationToken;
import com.foodDelivery.userService.modal.PasswordResetToken;
import com.foodDelivery.userService.modal.Role;
import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.ConfirmationTokenRepository;
import com.foodDelivery.userService.repository.PasswordResetTokenRepository;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.serviceInterfaces.KafkaProducerService;
import com.foodDelivery.userService.serviceInterfaces.UserService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final KafkaProducerService kafkaProducerService;
    private static final String CONFIRMATION_URL = "http://localhost:8081/api/auth/confirm?token=";

    @Override
    public Optional<UserProfileResponse> getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileResponse);
    }

    @Override
    public boolean validateUserRoleAndEnabled(Long userId, String role) {
        return userRepository.findById(userId)
                .map(user -> user.isEnabled() && user.getRoles().stream()
                        .anyMatch(userRole -> userRole.getName().equals(role)))
                .orElse(false);
    }

    @Override
    public UserProfileResponse getUserByUserName(String username){
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileResponse)
                .orElse(null);
    }

    @Override
    public boolean updateUserProfile(String username, UserProfileRequest profileRequest) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    // Validate unique fields
                    if (profileRequest.getUsername() != null && !profileRequest.getUsername().equals(user.getUsername())) {
                        if (userRepository.existsByUsername(profileRequest.getUsername())) {
                            throw new IllegalArgumentException("Username already taken");
                        }
                        user.setUsername(profileRequest.getUsername());
                    }

                    if (profileRequest.getEmail() != null && !profileRequest.getEmail().equals(user.getEmail())) {
                        if (userRepository.existsByEmail(profileRequest.getEmail())) {
                            throw new IllegalArgumentException("Email already in use");
                        }
                        user.setEmail(profileRequest.getEmail());
                    }

                    if (profileRequest.getPhoneNumber() != null && !profileRequest.getPhoneNumber().equals(user.getPhoneNumber())) {
                        if (userRepository.existsByPhoneNumber(profileRequest.getPhoneNumber())) {
                            throw new IllegalArgumentException("Phone number already in use");
                        }
                        user.setPhoneNumber(profileRequest.getPhoneNumber());
                    } else if (profileRequest.getPhoneNumber() != null) {
                        user.setPhoneNumber(profileRequest.getPhoneNumber());
                    }

                    // Update other fields
                    if (profileRequest.getFirstName() != null) {
                        user.setFirstName(profileRequest.getFirstName());
                    }

                    if (profileRequest.getLastName() != null) {
                        user.setLastName(profileRequest.getLastName());
                    }

                    if (profileRequest.getProfilePicture() != null) {
                        user.setProfileImage(profileRequest.getProfilePicture());
                    }

                    // Handle location if present
                    if (profileRequest.getLocation() != null) {
                        UserProfileRequest.LocationDTO locationDTO = profileRequest.getLocation();
                        user.setLocationType(locationDTO.getType());

                        if (locationDTO.getCoordinates() != null && locationDTO.getCoordinates().length == 2) {
                            user.setLongitude(locationDTO.getCoordinates()[0]);
                            user.setLatitude(locationDTO.getCoordinates()[1]);
                        }

                        if (locationDTO.getAddress() != null) {
                            user.setAddress(locationDTO.getAddress());
                        }
                    } else if (profileRequest.getAddress() != null) {
                        user.setAddress(profileRequest.getAddress());
                    }

                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean changePassword(String username, PasswordChangeRequest request) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAllUsers()
                .stream()
                .map(this::mapToUserProfileResponse)
                .toList();
    }

    @Override
    public UserProfileResponse createUserByAdmin(SignupRequest signUpRequest) {
        // Validate unique fields
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (signUpRequest.getPhoneNumber() != null && !signUpRequest.getPhoneNumber().isEmpty() &&
                userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        if (signUpRequest.getIdentificationNumber() != null && !signUpRequest.getIdentificationNumber().isEmpty() &&
                userRepository.existsByIdentificationNumber(signUpRequest.getIdentificationNumber())) {
            throw new IllegalArgumentException("Identification number is already registered");
        }

        // Create new user account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        // Handle user details
        user.setFirstName(signUpRequest.getFirstName() != null ? signUpRequest.getFirstName() : "");
        user.setLastName(signUpRequest.getLastName() != null ? signUpRequest.getLastName() : "");
        user.setPhoneNumber(signUpRequest.getPhoneNumber() != null ? signUpRequest.getPhoneNumber() : "");
        user.setProfileImage(signUpRequest.getProfilePicture() != null ? signUpRequest.getProfilePicture() : "");
        user.setAddress(signUpRequest.getAddress() != null ? signUpRequest.getAddress() : "");

        // Handle location information
        if (signUpRequest.getLocation() != null) {
            user.setLocationType(signUpRequest.getLocation().getType());
            if (signUpRequest.getLocation().getCoordinates() != null) {
                user.setLongitude(signUpRequest.getLocation().getCoordinates()[0]);
                user.setLatitude(signUpRequest.getLocation().getCoordinates()[1]);
            } else {
                user.setLongitude(0.0);
                user.setLatitude(0.0);
            }
        } else {
            user.setLocationType("");
            user.setLongitude(0.0);
            user.setLatitude(0.0);
        }

        // Set status fields for admin-created accounts
        user.setDisabled(false);
        user.setDeleted(false);
        user.setVerified(true);  // Admin-created accounts are pre-verified
        user.setEnabled(false);

        // Special fields for driver or restaurant admin accounts if applicable
        if (signUpRequest.getIdentificationNumber() != null) {
            user.setIdentificationNumber(signUpRequest.getIdentificationNumber());
        }
        if (signUpRequest.getVehicleNumber() != null) {
            user.setVehicleNumber(signUpRequest.getVehicleNumber());
        }

        // Handle roles - default to CUSTOMER if none specified
        Set<Role> roles = assignUserRoles(signUpRequest.getRoles());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate confirmation token and URL
        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);

        String confirmationUrl = CONFIRMATION_URL + confirmationToken.getToken();

        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .toList();

        // Send registration event
        UserRegistrationAdminEvent event = new UserRegistrationAdminEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                signUpRequest.getPassword(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                roleNames,
                "ADMIN_USER_REGISTERED",
                savedUser.getPhoneNumber(),
                confirmationUrl,
                System.currentTimeMillis()
        );

        kafkaProducerService.sendAdminUserRegistrationEvent(event);

        log.info("Admin created new user: {}, with roles: {}", savedUser.getUsername(),
                roles.stream().map(Role::getName).collect(Collectors.toList()));

        return mapToUserProfileResponse(savedUser);
    }

    private Set<Role> assignUserRoles(Set<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Error: Default role not found."));
            roles.add(customerRole);
            return roles;
        }

        for (String role : requestedRoles) {
            switch (role) {
                case "ROLE_ADMIN":
                    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                            .orElseThrow(() -> new RuntimeException("Error: Admin role not found."));
                    roles.add(adminRole);
                    break;
                case "ROLE_RESTAURANT_ADMIN":
                    Role restaurantRole = roleRepository.findByName("ROLE_RESTAURANT_ADMIN")
                            .orElseThrow(() -> new RuntimeException("Error: Restaurant role not found."));
                    roles.add(restaurantRole);
                    break;
                case "ROLE_DRIVER":
                    Role deliveryRole = roleRepository.findByName("ROLE_DELIVERY_PERSONNEL")
                            .orElseThrow(() -> new RuntimeException("Error: Delivery role not found."));
                    roles.add(deliveryRole);
                    break;
                default:
                    Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                            .orElseThrow(() -> new RuntimeException("Error: Default role not found."));
                    roles.add(customerRole);
            }
        }
        return roles;
    }

    @Override
    public UserProfileResponse updateUserByAdmin(Long userId, UpdateProfileRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Track changed fields for notification
        Map<String, String> changedFields = new HashMap<>();

        // Validate unique fields when they change (safely check for null values)
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(updateRequest.getUsername())) {
                throw new IllegalArgumentException("Username is already taken");
            }
            changedFields.put("Username", updateRequest.getUsername());
            user.setUsername(updateRequest.getUsername());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new IllegalArgumentException("Email is already in use");
            }
            changedFields.put("Email", updateRequest.getEmail());
            user.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (!updateRequest.getPhoneNumber().isEmpty() &&
                    userRepository.existsByPhoneNumber(updateRequest.getPhoneNumber())) {
                throw new IllegalArgumentException("Phone number is already registered");
            }
            changedFields.put("Phone Number", updateRequest.getPhoneNumber());
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getIdentificationNumber() != null &&
                !updateRequest.getIdentificationNumber().equals(user.getIdentificationNumber())) {
            if (!updateRequest.getIdentificationNumber().isEmpty() &&
                    userRepository.existsByIdentificationNumber(updateRequest.getIdentificationNumber())) {
                throw new IllegalArgumentException("Identification number is already registered");
            }
            changedFields.put("Identification Number", updateRequest.getIdentificationNumber());
            user.setIdentificationNumber(updateRequest.getIdentificationNumber());
        }

        // Only update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            changedFields.put("Password", "********");
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        // Update personal details
        if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().equals(user.getFirstName())) {
            changedFields.put("First Name", updateRequest.getFirstName());
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null && !updateRequest.getLastName().equals(user.getLastName())) {
            changedFields.put("Last Name", updateRequest.getLastName());
            user.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getProfilePicture() != null && !updateRequest.getProfilePicture().equals(user.getProfileImage())) {
            changedFields.put("Profile Picture", "Updated");
            user.setProfileImage(updateRequest.getProfilePicture());
        }

        if (updateRequest.getAddress() != null && !updateRequest.getAddress().equals(user.getAddress())) {
            changedFields.put("Address", updateRequest.getAddress());
            user.setAddress(updateRequest.getAddress());
        }

        // Update location information if provided
        if (updateRequest.getLocation() != null) {
            if (updateRequest.getLocation().getType() != null &&
                    !updateRequest.getLocation().getType().equals(user.getLocationType())) {
                changedFields.put("Location Type", updateRequest.getLocation().getType());
                user.setLocationType(updateRequest.getLocation().getType());
            }

            if (updateRequest.getLocation().getCoordinates() != null &&
                    updateRequest.getLocation().getCoordinates().length == 2) {
                if (user.getLongitude() != updateRequest.getLocation().getCoordinates()[0] ||
                        user.getLatitude() != updateRequest.getLocation().getCoordinates()[1]) {
                    changedFields.put("Location Coordinates",
                            "Long: " + updateRequest.getLocation().getCoordinates()[0] +
                                    ", Lat: " + updateRequest.getLocation().getCoordinates()[1]);
                    user.setLongitude(updateRequest.getLocation().getCoordinates()[0]);
                    user.setLatitude(updateRequest.getLocation().getCoordinates()[1]);
                }
            }
        }

        // Update vehicle number for drivers
        if (updateRequest.getVehicleNumber() != null && !updateRequest.getVehicleNumber().equals(user.getVehicleNumber())) {
            changedFields.put("Vehicle Number", updateRequest.getVehicleNumber());
            user.setVehicleNumber(updateRequest.getVehicleNumber());
        }

        // Update status fields if present
        if (updateRequest.getDisabled() != null && updateRequest.getDisabled() != user.isDisabled()) {
            changedFields.put("Account Status", updateRequest.getDisabled() ? "Disabled" : "Enabled");
            user.setDisabled(updateRequest.getDisabled());
        }

        if (updateRequest.getVerified() != null && updateRequest.getVerified() != user.isVerified()) {
            changedFields.put("Verification Status", updateRequest.getVerified() ? "Verified" : "Unverified");
            user.setVerified(updateRequest.getVerified());
        }

        // Update roles if specified
        Set<String> oldRoleNames = null;
        if (updateRequest.getRoles() != null && !updateRequest.getRoles().isEmpty()) {
            oldRoleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .map(name -> name.replace("ROLE_", ""))
                    .collect(Collectors.toSet());

            Set<Role> roles = assignUserRoles(updateRequest.getRoles());
            user.setRoles(roles);

            Set<String> newRoleNames = roles.stream()
                    .map(Role::getName)
                    .map(name -> name.replace("ROLE_", ""))
                    .collect(Collectors.toSet());

            if (!newRoleNames.equals(oldRoleNames)) {
                changedFields.put("Roles", String.join(", ", newRoleNames));
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Send notification to user about profile update if any fields changed
        if (!changedFields.isEmpty()) {
            sendProfileUpdateNotification(updatedUser, changedFields);
        }

        log.info("Admin updated user: {}, with roles: {}", updatedUser.getUsername(),
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        return mapToUserProfileResponse(updatedUser);
    }

    private void sendProfileUpdateNotification(User user, Map<String, String> changedFields) {
        // Create notification content
        StringBuilder changes = new StringBuilder();
        changedFields.forEach((field, value) ->
                changes.append("• ").append(field).append(": ").append(value).append("\n"));

        // Create notification event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("userId", user.getId());
        eventData.put("username", user.getUsername());
        eventData.put("email", user.getEmail());
        eventData.put("firstName", user.getFirstName());
        eventData.put("lastName", user.getLastName());
        eventData.put("phoneNumber", user.getPhoneNumber());
        eventData.put("changedFields", changedFields);
        eventData.put("eventType", "PROFILE_UPDATED_BY_ADMIN");
        eventData.put("timestamp", System.currentTimeMillis());

        // Send notification through Kafka
        kafkaProducerService.sendProfileUpdateNotification(eventData);

        log.info("Profile update notification sent to user {}: {}", user.getUsername(), changes.toString().trim());
    }

    @Override
    @CircuitBreaker(name = "passwordResetEmail", fallbackMethod = "passwordResetEmailFallback")
    public boolean requestPasswordReset(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setToken(UUID.randomUUID().toString());
                    token.setExpiryDate(LocalDateTime.now().plusHours(24));
                    passwordResetTokenRepository.save(token);

                    // Here you would send a notification through Kafka
                    // Similar to how you're doing it with registration

                    return true;
                })
                .orElse(false);
    }

    public boolean passwordResetEmailFallback(String email, Exception e) {
        log.error("Failed to process password reset for {}: {}", email, e.getMessage());
        return false;
    }

    @Override
    public boolean resetPassword(PasswordResetRequest resetRequest) {
        return passwordResetTokenRepository.findByToken(resetRequest.getToken())
                .filter(token -> !token.isExpired())
                .map(token -> {
                    User user = token.getUser();
                    user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
                    userRepository.save(user);
                    passwordResetTokenRepository.delete(token);
                    return true;
                })
                .orElse(false);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getProfileImage(),
                user.getAddress(),
                user.getLocationType(),
                user.getLatitude(),
                user.getLongitude(),
                user.isEnabled(),
                user.isDisabled(),
                user.isDeleted(),
                user.isVerified(),
                user.getIdentificationNumber(),
                user.getVehicleNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList()
        );
    }

    @Override
    public boolean validateUserRole(Long userId, String role) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .anyMatch(userRole -> userRole.getName().equals(role)))
                .orElse(false);
    }

    @Override
    public boolean validateUserRoleAndEnabledByUsername(String username, String role) {
        return userRepository.findByUsername(username)
                .map(user -> user.isEnabled() && user.getRoles().stream()
                        .anyMatch(userRole -> userRole.getName().equals(role)))
                .orElse(false);
    }

    @Override
    public List<UserProfileResponse> getUsersByRole(String roleName) {
        // Ensure role name format is correct
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        // Find the role
        Optional<Role> role = roleRepository.findByName(roleName);
        if (role.isEmpty()) {
            return Collections.emptyList();
        }

        // Get users with this role
        List<User> users = userRepository.findByRolesContaining(role.get());

        // Map to response objects
        return users.stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserProfileResponse getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::mapToUserProfileResponse)
                .orElse(null);
    }

    @Override
    public Optional<Long> findIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId);
    }


}