package com.foodDelivery.userService.serviceImplementation;

import com.foodDelivery.userService.config.JwtUtils;
import com.foodDelivery.userService.dto.GoogleAuthRequest;
import com.foodDelivery.userService.dto.JwtResponse;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.userService.modal.ConfirmationToken;
import com.foodDelivery.userService.modal.Role;
import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.ConfirmationTokenRepository;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.serviceInterfaces.GoogleAuthService;
import com.foodDelivery.userService.serviceInterfaces.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final KafkaProducerService kafkaProducerService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    @Transactional
    public JwtResponse processGoogleAuth(GoogleAuthRequest request) {
        log.info("Processing Google authentication for email: {}", request.getEmail());

        Optional<User> existingUser = userRepository.findByEmail(request.getEmail().toLowerCase().trim());

        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            log.info("Existing user found with email: {}", request.getEmail());
            user = existingUser.get();

            // Check if user is enabled
            if (!user.isEnabled()) {
                log.warn("User account not verified, enabling it since Google has verified the email: {}", user.getUsername());
                user.setEnabled(true);
                userRepository.save(user);
            }

            // Update profile information if needed
            if (request.getGooglePhotoURL() != null && !request.getGooglePhotoURL().equals(user.getProfileImage())) {
                user.setProfileImage(request.getGooglePhotoURL());
                userRepository.save(user);
                log.info("Updated profile picture for user: {}", user.getUsername());
            }
        } else {
            log.info("Creating new user account for Google user: {}", request.getEmail());
            user = createGoogleUser(request);
            isNewUser = true;
        }

        // Create UserDetails and authenticate the user
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token using authentication
        String jwt = jwtUtils.generateJwtToken(authentication);
        log.info("JWT token generated for Google user: {}", user.getUsername());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new JwtResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles);
    }

    @Transactional
    private User createGoogleUser(GoogleAuthRequest request) {
        User user = new User();

        // Set basic user info
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setUsername(generateUsername(request.getName()));
        user.setPassword(passwordEncoder.encode(generateRandomPassword()));
        user.setProfileImage(request.getGooglePhotoURL());
        user.setFirstName(extractFirstName(request.getName()));
        user.setLastName(extractLastName(request.getName()));

        // Google authenticated users are automatically enabled
        user.setEnabled(true);

        // Set default role as CUSTOMER
        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> {
                    log.error("Default ROLE_CUSTOMER not found in database");
                    return new RuntimeException("Default role ROLE_CUSTOMER not found");
                });
        user.setRoles(Set.of(customerRole));

        // Save the user
        User savedUser = userRepository.save(user);
        log.info("New Google user created with username: {}", user.getUsername());

        // Create confirmation token (for tracking purposes)
        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);
        log.info("Confirmation token created for Google user: {}", confirmationToken.getToken());

        // Publish user registration event
        publishUserRegistrationEvent(savedUser, "GOOGLE_AUTH");

        return savedUser;
    }

    private void publishUserRegistrationEvent(User user, String confirmationUrl) {
        UserRegistrationEvent event = new UserRegistrationEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                "GOOGLE_USER_REGISTERED",
                user.getPhoneNumber(),
                confirmationUrl,
                System.currentTimeMillis()
        );

        kafkaProducerService.sendUserRegistrationEvent(event);
        log.info("User registration event published for Google user: {}", user.getUsername());
    }

    private String generateUsername(String name) {
        String baseName = name.toLowerCase().replace(" ", "");
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String username = baseName + randomSuffix;

        // Check if username exists and regenerate if needed
        int attempts = 0;
        while (userRepository.existsByUsername(username) && attempts < 5) {
            randomSuffix = String.format("%04d", new Random().nextInt(10000));
            username = baseName + randomSuffix;
            attempts++;
        }

        if (attempts >= 5) {
            // If we still have conflicts after 5 attempts, add more randomness
            username = baseName + System.currentTimeMillis() % 10000;
        }

        return username;
    }

    private String generateRandomPassword() {
        String upperChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerChars = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()";
        String allChars = upperChars + lowerChars + numbers + specialChars;

        StringBuilder password = new StringBuilder();
        Random random = new Random();

        // Ensure at least one character from each category for stronger password
        password.append(upperChars.charAt(random.nextInt(upperChars.length())));
        password.append(lowerChars.charAt(random.nextInt(lowerChars.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill the rest of the password
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int j = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    private String extractFirstName(String fullName) {
        return fullName.contains(" ") ? fullName.split(" ")[0] : fullName;
    }

    private String extractLastName(String fullName) {
        if (!fullName.contains(" ")) {
            return "";
        }
        return fullName.substring(fullName.indexOf(" ") + 1);
    }
}