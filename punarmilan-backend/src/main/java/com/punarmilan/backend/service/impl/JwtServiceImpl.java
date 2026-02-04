package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.security.JwtUtil;
import com.punarmilan.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtUtil jwtUtil;

    @Override
    public String generateToken(String email) {
        return jwtUtil.generateToken(email, "USER");
    }


    @Override
    public String extractUsername(String token) {
        return jwtUtil.extractEmail(token);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && isTokenValid(token);
    }

    @Override
    public boolean isTokenValid(String token) {
        return jwtUtil.isTokenValid(token);
    }
}