package com.example.interhubdev.auth.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the application.
 * 
 * <p>Package-private: internal implementation detail of the auth module.</p>
 * 
 * <p>Configures JWT-based stateless authentication with HttpOnly cookies.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AuthProperties authProperties;

    /**
     * Password encoder using BCrypt algorithm.
     * Used for hashing user passwords.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain configuration.
     * Uses JWT authentication with tokens stored in HttpOnly cookies.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF - we use SameSite=Strict cookies for protection
            .csrf(csrf -> csrf.disable())
            
            // Security response headers
            .headers(headers -> {
                headers.frameOptions(this::applyFrameOptions);
                headers.contentTypeOptions(cto -> {});
                var hsts = authProperties.getSecurityHeaders();
                if (hsts.getHstsMaxAgeSeconds() > 0) {
                    headers.httpStrictTransportSecurity(h -> h
                            .maxAgeInSeconds(hsts.getHstsMaxAgeSeconds())
                            .includeSubDomains(hsts.isHstsIncludeSubdomains()));
                }
            })
            
            // Stateless session management (JWT-based)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Rate limit first, then JWT (both before UsernamePasswordAuthenticationFilter which has registered order)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - authentication
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                
                // Public endpoints - invitation acceptance (user activation)
                .requestMatchers("/api/invitations/validate", "/api/invitations/accept").permitAll()
                
                // Public endpoints - documentation
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Public endpoints - health checks
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    private void applyFrameOptions(HeadersConfigurer<?>.FrameOptionsConfig config) {
        String value = authProperties.getSecurityHeaders().getFrameOptions();
        if ("DENY".equalsIgnoreCase(value)) {
            config.deny();
        } else if ("SAMEORIGIN".equalsIgnoreCase(value)) {
            config.sameOrigin();
        } else {
            config.deny();
        }
    }
}
