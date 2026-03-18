package com.backend.api.auth.login;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.api.config.SupabaseConfig;
import com.backend.api.entity.User;
import com.backend.api.repository.UserRepository;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public LoginService(UserRepository userRepository,
                        SupabaseConfig config,
                        RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public Object login(String email, String password) {
        try {
            // Step 1: Check user exists in DB via JPA
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return Map.of("success", false, "error", "User not found");
            }

            // Step 2: Authenticate via Supabase Auth to get access_token
            String url = config.supabaseUrl + "/auth/v1/token?grant_type=password";
            Map<String, String> authBody = new HashMap<>();
            authBody.put("email", email);
            authBody.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> authResponse = (Map<String, Object>)
                    restTemplate.postForObject(url, entity, Object.class);

            // Step 3: Return token + DB user info
            User user = userOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("access_token", authResponse.get("access_token"));
            response.put("token_type", "bearer");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", "Invalid email or password");
        }
    }
}
