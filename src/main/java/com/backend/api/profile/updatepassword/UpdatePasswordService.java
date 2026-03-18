package com.backend.api.profile.updatepassword;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.api.config.SupabaseConfig;
import com.backend.api.repository.UserRepository;

@Service
public class UpdatePasswordService {

    private final UserRepository userRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public UpdatePasswordService(UserRepository userRepository,
                                  SupabaseConfig config,
                                  RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public Object updatePassword(String token, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            return Map.of("success", false, "error", "Password cannot be empty");
        }
        if (newPassword.length() < 6) {
            return Map.of("success", false, "error", "Password must be at least 6 characters");
        }

        try {
            // Step 1: Update password in Supabase Auth via API
            String url = config.supabaseUrl + "/auth/v1/user";
            Map<String, String> authBody = new HashMap<>();
            authBody.put("password", newPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authBody, headers);

            // Get user email from Supabase Auth response
            @SuppressWarnings("unchecked")
            Map<String, Object> authUser = (Map<String, Object>)
                    restTemplate.exchange(config.supabaseUrl + "/auth/v1/user",
                            HttpMethod.GET, new HttpEntity<>(headers), Object.class).getBody();

            restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);

            // Step 2: Update password in DB via JPA
            String email = (String) authUser.get("email");
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setPassword(newPassword);
                userRepository.save(user);
            });

            return Map.of("success", true, "message", "Password updated successfully");

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
