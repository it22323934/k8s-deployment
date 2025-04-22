package com.foodDelivery.userService.serviceImplementation;

import com.foodDelivery.userService.config.JwtUtils;
import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.userService.modal.ConfirmationToken;
import com.foodDelivery.userService.modal.PasswordResetToken;
import com.foodDelivery.userService.modal.Role;
import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.ConfirmationTokenRepository;
import com.foodDelivery.userService.repository.PasswordResetTokenRepository;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.serviceInterfaces.AuthService;
import com.foodDelivery.userService.serviceInterfaces.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final KafkaProducerService kafkaProducerService;

    private static final String RESET_PASSWORD_URL = "http://localhost:5173/reset-password?token=";
    private static final String CONFIRMATION_URL = "http://localhost:8081/api/auth/confirm?token=";

    @Override
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        try {
            // Determine login identifier (username or email)
            String loginIdentifier = loginRequest.getUsername();
            if ((loginIdentifier == null || loginIdentifier.isEmpty()) &&
                    loginRequest.getEmail() != null && !loginRequest.getEmail().isEmpty()) {
                loginIdentifier = loginRequest.getEmail();
            }

            log.info("Attempting authentication with identifier: {}", loginIdentifier);

            // Authenticate credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, loginRequest.getPassword()));

            log.info("Authentication successful for: {}", loginIdentifier);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Check if user exists and is enabled
            Optional<User> userOptional = userRepository.findById(userDetails.getId());
            if (userOptional.isEmpty()) {
                log.error("User not found in database after authentication: {}", userDetails.getId());
                return ResponseEntity.status(401)
                        .body(new MessageResponse("Error: User account not found."));
            }

            User user = userOptional.get();
            if (!user.isEnabled()) {
                log.info("User account not verified: {}", loginIdentifier);
                return ResponseEntity.status(403)
                        .body(new MessageResponse("Error: Account is not verified. Please check your email to verify your account."));
            }

            // Check if user is disabled or deleted
            if (user.isDisabled()) {
                log.info("User account is disabled: {}", loginIdentifier);
                return ResponseEntity.status(403)
                        .body(new MessageResponse("Error: Account is disabled. Please contact support."));
            }

            if (user.isDeleted()) {
                log.info("User account is deleted: {}", loginIdentifier);
                return ResponseEntity.status(403)
                        .body(new MessageResponse("Error: Account is deleted. Please contact support."));
            }

            // Continue with successful authentication
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(),
                    userDetails.getUsername(), userDetails.getEmail(), roles));
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                    .body(new MessageResponse("Error: Invalid credentials. Please check your email/username and password."));
        }
    }

    @Override
    public ResponseEntity<?> registerUser(SignupRequest signUpRequest) {
        // Validate required fields
        if (signUpRequest.getUsername() == null || signUpRequest.getEmail() == null || signUpRequest.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username, email and password are required!"));
        }

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (signUpRequest.getPhoneNumber() != null && !signUpRequest.getPhoneNumber().isEmpty() &&
                userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Phone number is already registered!"));
        }

        // Create new user's account with defaults for missing fields
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        // Handle optional fields with defaults
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

        // Set default account status values
        user.setDisabled(false);
        user.setDeleted(false);
        user.setVerified(false);
        user.setIdentificationNumber("");
        user.setVehicleNumber("");

        // Always assign only CUSTOMER role for self-registration
        Set<Role> roles = new HashSet<>();
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Default customer role not found."));
        roles.add(customerRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate confirmation token and URL
        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);

        String confirmationUrl = CONFIRMATION_URL + confirmationToken.getToken();

        // Send registration event
        UserRegistrationEvent event = new UserRegistrationEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                "USER_REGISTERED",
                savedUser.getPhoneNumber(),
                confirmationUrl,
                System.currentTimeMillis()
        );

        kafkaProducerService.sendUserRegistrationEvent(event);

        return ResponseEntity.ok(new MessageResponse("User registered successfully! Please check your email to verify your account."));
    }

    @Override
    public ResponseEntity<?> confirmUserAccount(String confirmationToken) {
        Optional<ConfirmationToken> token = confirmationTokenRepository.findByToken(confirmationToken);

        if (token.isPresent()) {
            User user = token.get().getUser();
            user.setEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Email confirmed successfully!"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid token!"));
        }
    }

    @Override
    public ResponseEntity<?> forgotPassword(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Generate password reset token
            String token = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setToken(token);
            passwordResetToken.setUser(user);
            passwordResetToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            passwordResetTokenRepository.save(passwordResetToken);

            // Create reset URL
            String resetUrl = RESET_PASSWORD_URL + token;

            // Send password reset event through Kafka
            kafkaProducerService.sendPasswordResetEvent(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    "PASSWORD_RESET_REQUESTED",
                    resetUrl
            );
        }

        // Always return success for security reasons
        return ResponseEntity.ok(new MessageResponse(
                "If the email exists in our system, password reset instructions have been sent."));
    }

    @Override
    public ResponseEntity<?> validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);

        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            if (resetToken.isExpired()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Token has expired!"));
            }

            return ResponseEntity.ok(new MessageResponse("Token is valid"));
        }

        return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Invalid token!"));
    }

    @Override
    public ResponseEntity<?> resetPassword(PasswordResetRequest resetRequest) {
        // Add proper logging
        log.info("Password reset requested with token: '{}'", resetRequest.getToken());
        String cleanToken = resetRequest.getToken().trim();
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(cleanToken);
        log.info("Token found in database: {}", tokenOptional.isPresent());
        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            if (resetToken.isExpired()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Token has expired!"));
            }

            User user = resetToken.getUser();
            user.setPassword(encoder.encode(resetRequest.getNewPassword()));
            userRepository.save(user);

            // Delete used token
            passwordResetTokenRepository.delete(resetToken);

            return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
        }

        return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Invalid token!"));
    }
}