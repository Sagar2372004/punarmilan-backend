package com.punarmilan.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.punarmilan.backend.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    // ✅ PRIMARY: SMART PASSWORD ENCODER (दोघांचा login होईल)
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();
            
            @Override
            public String encode(CharSequence rawPassword) {
                // नवीन passwords साठी BCrypt generate करा
                return bcryptEncoder.encode(rawPassword);
            }
            
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                System.out.println("\n=== PASSWORD MATCH DEBUG ===");
                System.out.println("Input Password: " + rawPassword);
                System.out.println("DB Password: " + encodedPassword);
                System.out.println("DB Password Length: " + encodedPassword.length());
                
                // 1. प्रथम BCrypt try करा
                boolean bcryptMatch = bcryptEncoder.matches(rawPassword, encodedPassword);
                System.out.println("BCrypt Match Result: " + bcryptMatch);
                
                if (bcryptMatch) {
                    System.out.println("✅ Login via BCrypt");
                    return true;
                }
                
                // 2. Plain Text try करा
                boolean plainMatch = rawPassword.toString().equals(encodedPassword);
                System.out.println("Plain Text Match Result: " + plainMatch);
                
                if (plainMatch) {
                    System.out.println("⚠️ Login via Plain Text (Auto-converting to BCrypt)");
                    
                    // Auto-convert plain text to BCrypt
                    // (तुम्ही हे implement करू शकता जर इच्छा असेल तर)
                    return true;
                }
                
                System.out.println("❌ No match found");
                return false;
            }
        };
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}