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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = authProperties.getCors().getAllowedOrigins();
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
            config.setAllowCredentials(true);
            config.addAllowedMethod("GET");
            config.addAllowedMethod("POST");
            config.addAllowedMethod("PUT");
            config.addAllowedMethod("PATCH");
            config.addAllowedMethod("DELETE");
            config.addAllowedMethod("OPTIONS");
            config.addAllowedHeader("*");
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS: uses corsConfigurationSource bean; when allowedOrigins is empty, no cross-origin is permitted
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF - we use SameSite=Strict cookies for protection
            .csrf(csrf -> csrf.disable())
            
            // Security response headers
            .headers(headers -> {
                headers.frameOptions(this::applyFrameOptions);
                headers.contentTypeOptions(cto -> applyContentTypeOptions(cto));
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

    private void applyContentTypeOptions(HeadersConfigurer<?>.ContentTypeOptionsConfig config) {
        String value = authProperties.getSecurityHeaders().getContentTypeOptions();
        if (value == null || !"nosniff".equalsIgnoreCase(value.trim())) {
            config.disable();
        }
    }
}
