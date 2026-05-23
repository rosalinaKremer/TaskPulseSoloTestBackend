package com.backend.api.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                headers.set("apikey", supabaseKey);

                ResponseEntity<Map> response = restTemplate.exchange(
                    supabaseUrl + "/auth/v1/user",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    String email = (String) response.getBody().get("email");
                    if (email != null) {
                        var auth = new UsernamePasswordAuthenticationToken(
                            email, null, List.of()
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                // Invalid token — leave SecurityContext empty, Spring will 403
            }
        }
        chain.doFilter(req, res);
    }
}