package com.edtech.lms.identity.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.edtech.lms.identity.repositories.UserRepository;
import com.edtech.lms.identity.models.entities.User;
import java.util.ArrayList;

/**
 * CustomUserDetailsService - Loads user details from database
 * 
 * Spring Security Flow:
 * 1. JwtAuthenticationFilter creates authentication token with username
 * 2. Spring calls loadUserByUsername() to load user from database
 * 3. Compares password (if login flow) or uses existing password hash
 * 4. Returns UserDetails with roles/permissions
 * 
 * For JWT flow (stateless):
 * - Username already verified by JWT signature
 * - This service just loads user details for authorization checks
 * - No password comparison needed (JWT signature already proves identity)
 * 
 * In Phase 2, when implementing login endpoints:
 * - This service will be used to load user for password comparison
 * - authenticationManager.authenticate() will call this service
 * 
 * @Service annotation:
 * - Marks this as a Spring service bean
 * - Can be autowired into other components
 * - Manages transactional boundaries
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user by username (email)
     * 
     * Called by Spring Security during authentication
     * Used for both JWT and login flows
     * 
     * @param username User's email address
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Query database for user by email
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // Convert our User entity to Spring's UserDetails
        // For now, we don't include roles in authorities (will implement in Phase 2)
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(new ArrayList<>())  // Add roles in Phase 2
                .build();
    }
}
