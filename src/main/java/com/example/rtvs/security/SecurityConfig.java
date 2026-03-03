package com.example.rtvs.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * Stateless (JWT) — no HTTP session is created.
 * Method-level security (@PreAuthorize) is enabled via @EnableMethodSecurity.
 *
 * Route-level access rules:
 *   POST /api/v1/rtp/payments          → USER
 *   GET  /api/v1/rtp/users/{id}/**     → USER (own data enforced in service layer)
 *   GET  /api/v1/rtp/transactions/failed → ANALYST, ADMIN
 *   All other /api/** endpoints        → authenticated
 *   /auth/**, /h2-console/**, Swagger  → public
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(jwtAuthEntryPoint))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // Payment initiation — USER only
                .requestMatchers(HttpMethod.POST, "/api/v1/rtp/payments")
                        .hasAuthority("ROLE_USER")

                // Balance & history — USER only (ownership enforced in service)
                .requestMatchers(HttpMethod.GET, "/api/v1/rtp/users/**")
                        .hasAuthority("ROLE_USER")

                // Failed transactions log — ANALYST or ADMIN
                .requestMatchers(HttpMethod.GET, "/api/v1/rtp/transactions/failed")
                        .hasAnyAuthority("ROLE_ANALYST", "ROLE_ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        // Allow H2 console frames in dev
        http.headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
