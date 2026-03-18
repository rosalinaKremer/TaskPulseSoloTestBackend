package com.backend.api.profile.updateprofile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.api.config.SupabaseConfig;
import com.backend.api.entity.Profile;
import com.backend.api.entity.User;
import com.backend.api.repository.ProfileRepository;
import com.backend.api.repository.UserRepository;

@Service
public class UpdateProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public UpdateProfileService(ProfileRepository profileRepository,
                                UserRepository userRepository,
                                SupabaseConfig config,
                                RestTemplate restTemplate) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public Object updateProfile(String email, Map<String, String> body) {
        try {
            // Step 1: Find user
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return Map.of("success", false, "error", "User not found with email: " + email);
            }

            User user = userOpt.get();

            // Step 2: Find profile — auto-create if missing
            Optional<Profile> profileOpt = profileRepository.findByUserEmail(email);
            Profile profile;

            if (profileOpt.isEmpty()) {
                profile = new Profile();
                profile.setUser(user);
            } else {
                profile = profileOpt.get();
            }

            // Step 3: Update only provided fields
            if (body.containsKey("username")) profile.setUsername(body.get("username"));
            if (body.containsKey("fullName")) profile.setFullName(body.get("fullName"));
            if (body.containsKey("bio"))      profile.setBio(body.get("bio"));

            profile = profileRepository.save(profile);

            // Mirror to Supabase public.profiles so updates are visible in Table Editor.
            String supabaseSyncError = syncProfileToSupabasePublic(user, profile);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",   true);
            response.put("message",   "Profile updated successfully");
            response.put("username",  profile.getUsername());
            response.put("fullName",  profile.getFullName());
            response.put("bio",       profile.getBio());
            response.put("updatedAt", profile.getUpdatedAt());
            response.put("syncedToSupabase", supabaseSyncError == null);
            if (supabaseSyncError != null) {
                response.put("supabaseSyncError", supabaseSyncError);
            }
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private String syncProfileToSupabasePublic(User user, Profile profile) {
        try {
            String email = user.getEmail();
            String userId = resolveSupabaseAuthUserId(email, user.getPassword());
            if (userId == null || userId.isBlank()) {
                userId = user.getId().toString();
            }
            String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            String profilesUpdateUrl = config.supabaseUrl + "/rest/v1/profiles?id=eq." + encodedUserId;
            String profilesInsertUrl = config.supabaseUrl + "/rest/v1/profiles";

            Map<String, Object> profilePayload = new LinkedHashMap<>();
            profilePayload.put("id", userId);
            profilePayload.put("email", email);
            profilePayload.put("username", profile.getUsername());
            profilePayload.put("full_name", profile.getFullName());
            profilePayload.put("bio", profile.getBio());
            profilePayload.put("photo", profile.getPhoto());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseKey);
            headers.set("Authorization", "Bearer " + config.supabaseKey);
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> profileEntity = new HttpEntity<>(profilePayload, headers);

            // Update profile by id (linked to users.id), insert if not found.
            String profileUpdateBody = restTemplate.exchange(profilesUpdateUrl, HttpMethod.PATCH, profileEntity, String.class).getBody();
            if (profileUpdateBody == null || "[]".equals(profileUpdateBody.trim())) {
                restTemplate.exchange(profilesInsertUrl, HttpMethod.POST, profileEntity, String.class);
            }
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveSupabaseAuthUserId(String email, String password) {
        try {
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return null;
            }

            String url = config.supabaseUrl + "/auth/v1/token?grant_type=password";

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            Map<String, Object> authResponse = (Map<String, Object>) restTemplate.postForObject(url, entity, Object.class);
            if (authResponse == null) {
                return null;
            }

            Object userObj = authResponse.get("user");
            if (!(userObj instanceof Map<?, ?> userMap)) {
                return null;
            }

            Object idObj = userMap.get("id");
            return idObj != null ? idObj.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}