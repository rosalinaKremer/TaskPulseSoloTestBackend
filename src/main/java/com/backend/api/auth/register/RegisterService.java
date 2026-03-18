package com.backend.api.auth.register;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.api.config.SupabaseConfig;
import com.backend.api.entity.Profile;
import com.backend.api.entity.User;
import com.backend.api.repository.ProfileRepository;
import com.backend.api.repository.UserRepository;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public RegisterService(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           SupabaseConfig config,
                           RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public Object register(String email, String password) {
        try {
            // Step 1: Check if email already exists in DB
            if (userRepository.existsByEmail(email)) {
                return Map.of("success", false, "error", "Email already registered");
            }

            // Step 2: Register in Supabase Auth Module
            String url = config.supabaseUrl + "/auth/v1/signup";
            Map<String, String> authBody = new HashMap<>();
            authBody.put("email", email);
            authBody.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authBody, headers);
            restTemplate.postForObject(url, entity, Object.class);

            // Step 3: Save user in DB via JPA
            User user = new User();
            user.setEmail(email);
            user.setPassword(password); // store as-is (Supabase Auth handles real hashing)
            User savedUser = userRepository.save(user);

            // Step 4: Create empty profile linked to the user
            Profile profile = new Profile();
            profile.setUser(savedUser);
            profileRepository.save(profile);

            // Step 5: Return response
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("userId", savedUser.getId());
            response.put("email", savedUser.getEmail());
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
