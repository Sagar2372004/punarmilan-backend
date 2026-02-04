package com.punarmilan.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.punarmilan.backend.security.JwtAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for APIs and WebSocket
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/verify-email-update",
                                "/api/auth/reset-password",
                                "/api/auth/forgot-password",
                                "/api/public/**", // Public API endpoints
                                "/api/payments/webhook/**", // Payment webhooks
                                "/v3/api-docs/**", // Swagger API docs
                                "/swagger-ui/**", // Swagger UI
                                "/swagger-ui.html", // Swagger UI HTML
                                "/uploads/**", // Uploaded files
                                "/ws/**", // WebSocket endpoints
                                "/ws", // WebSocket endpoint
                                "/app/**", // WebSocket message mappings
                                "/topic/**", // WebSocket topics
                                "/queue/**", // WebSocket queues
                                "/user/**", // User-specific WebSocket
                                "/actuator/health", // Health check
                                "/error" // Error endpoint
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // User endpoints
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        // Messaging endpoints
                        .requestMatchers("/api/messaging/**").hasAnyRole("USER", "ADMIN")

                        // Notification endpoints
                        .requestMatchers("/api/notifications/**").hasAnyRole("USER", "ADMIN")

                        // Payment endpoints
                        .requestMatchers("/api/payments/**").hasAnyRole("USER", "ADMIN")

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:4200",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));

        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcryptEncoder = new BCryptPasswordEncoder();

            @Override
            public String encode(CharSequence rawPassword) {
                return bcryptEncoder.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                // First try BCrypt
                if (bcryptEncoder.matches(rawPassword, encodedPassword)) {
                    return true;
                }

                // Fallback: For already existing plain text passwords
                // This is temporary for migration
                try {
                    // Check if it's a BCrypt hash (starts with $2a$, $2b$, $2y$)
                    if (encodedPassword.startsWith("$2a$") ||
                            encodedPassword.startsWith("$2b$") ||
                            encodedPassword.startsWith("$2y$")) {
                        return false; // It's a BCrypt hash but didn't match
                    }

                    // Plain text comparison (remove this after migration)
                    return rawPassword.toString().equals(encodedPassword);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    @Bean
    public BCryptPasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}