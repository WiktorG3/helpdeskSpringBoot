package com.example.helpdesk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity in development
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/register").permitAll() // Allow unauthenticated access to /api/register
                        .anyRequest().authenticated() // Protect all other endpoints
                )
                .httpBasic(httpBasic -> httpBasic.realmName("Helpdesk API")); // Configure HTTP Basic authentication
        return http.build();
    }
}