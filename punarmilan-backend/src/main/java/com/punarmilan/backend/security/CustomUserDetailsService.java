package com.punarmilan.backend.security;

import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));

        // ✅ FIX: ROLE_ prefix ADD करा
        String roleWithPrefix = "ROLE_" + user.getRole().toUpperCase();
        
        List<SimpleGrantedAuthority> authorities = 
            Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),          // enabled
                true,                     // accountNonExpired
                true,                     // credentialsNonExpired
                true,                     // accountNonLocked
                authorities
        );
    }
}