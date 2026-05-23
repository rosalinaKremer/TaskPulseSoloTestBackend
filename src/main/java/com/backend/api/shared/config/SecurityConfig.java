package com.backend.api.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults()) // Uses your existing CorsConfig
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
            .authorizeHttpRequests(auth -> auth
                // 1. Allow public access to static PWA files that were causing 401s
                .requestMatchers("/manifest.json", "/favicon.ico", "/logo192.png", "/logo512.png", "/index.html", "/").permitAll()
                // 2. Allow public access to your Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // 3. Everything else requires authentication
                .requestMatchers("/api/tasks/available").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}