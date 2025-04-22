package com.foodDelivery.userService.serviceImplementation;

import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // First try to find by username
        User user = userRepository.findByUsername(usernameOrEmail)
                .orElse(null);

        // If not found by username, try by email
        if (user == null) {
            user = userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with username or email: " + usernameOrEmail));
        }

        return UserDetailsImpl.build(user);
    }
}