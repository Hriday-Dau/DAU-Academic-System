package com.ecampus.auth.service;

import com.ecampus.auth.user.AuthUserDetails;
import com.ecampus.auth.user.UserDetailsRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserDetailsRepository repository;

    public DatabaseUserDetailsService(UserDetailsRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        AuthUserDetails user = repository.findWithName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = user.getrole();

        if (role == null || role.isBlank() || "UNKNOWN".equals(role)) {
            throw new UsernameNotFoundException("Invalid role for user: " + username);
        }

        return User.builder()
                .username(username)
                .password(user.getpassword())
                .authorities(role)
                .build();
    }
}