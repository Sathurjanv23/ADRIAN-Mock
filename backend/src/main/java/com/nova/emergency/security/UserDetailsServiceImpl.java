package com.nova.emergency.security;

import com.nova.emergency.model.User;
import com.nova.emergency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        boolean enabled = user.isActive() && !"DEACTIVATED".equalsIgnoreCase(user.getStatus());
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "";
        String roleAuthority = "ROLE_" + (user.getRole() != null ? user.getRole().toUpperCase() : "CITIZEN");

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                enabled,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(roleAuthority))
        );
    }
}
