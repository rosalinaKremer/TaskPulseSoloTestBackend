package com.backend.api.features.auth.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.api.features.auth.model.User;
import com.backend.api.features.auth.repository.UserRepository;
import com.backend.api.features.profile.model.Profile;
import com.backend.api.features.profile.repository.ProfileRepository;
import com.backend.api.shared.config.SupabaseConfig;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       SupabaseConfig config,
                       RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public Object login(String email, String password) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return Map.of("success", false, "error", "User not found");
            }

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

            User user = userOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("access_token", authResponse.get("access_token"));
            response.put("token_type", "bearer");
            
            // ── CHANGED HERE: Wrap user information inside a structured "user" map ──
            // This mirrors exactly what your client-side Login.jsx expects: data.user.role
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole()); // Sends "user" or "admin" from local DB
            
            response.put("user", userData);
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", "Invalid email or password");
        }
    }

    public Object register(String email, String password) {
        try {
            if (userRepository.existsByEmail(email)) {
                return Map.of("success", false, "error", "Email already registered");
            }

            String url = config.supabaseUrl + "/auth/v1/signup";
            Map<String, String> authBody = new HashMap<>();
            authBody.put("email", email);
            authBody.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authBody, headers);
            restTemplate.postForObject(url, entity, Object.class);

            // 1. Create and save the User
            User user = new User();
            user.setEmail(email);
            user.setPassword(password); 
            // Note: user.setRole("user") is automatically handled by the field default value we added
            User savedUser = userRepository.save(user);

            // 2. Create and correctly populate the Profile
            Profile profile = new Profile();
            profile.setUser(savedUser);
            profile.setEmail(email);
            
            String defaultName = email.substring(0, email.indexOf("@"));
            profile.setUsername(defaultName);
            profile.setFullName(defaultName);
            
            profileRepository.save(profile);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            
            // ── CHANGED HERE: Also bundle user and role profiles neatly on creation ──
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("id", savedUser.getId());
            userData.put("email", savedUser.getEmail());
            userData.put("role", savedUser.getRole());
            
            response.put("user", userData);
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}