package com.backend.api.features.profile.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

import com.backend.api.features.auth.model.User;
import com.backend.api.features.auth.repository.UserRepository;
import com.backend.api.features.profile.model.Profile;
import com.backend.api.features.profile.repository.ProfileRepository;
import com.backend.api.shared.config.SupabaseConfig;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final SupabaseConfig config;
    private final RestTemplate restTemplate;

    public ProfileService(ProfileRepository profileRepository,
                          UserRepository userRepository,
                          SupabaseConfig config,
                          RestTemplate restTemplate) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.config = config;
        this.restTemplate = restTemplate;
    }

    // ── 1. GET PROFILE ──
    // ── 1. GET PROFILE ──
    public Object getProfile(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) return Map.of("success", false, "error", "User not found");

            User user = userOpt.get();
            Optional<Profile> profileOpt = profileRepository.findByUserEmail(email);
            Profile profile;

            // Using standard if/else instead of lambdas
            if (profileOpt.isEmpty()) {
                profile = new Profile();
                profile.setUser(user);
                profile = profileRepository.save(profile);
            } else {
                profile = profileOpt.get();
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",   true);
            response.put("id",        profile.getId());
            response.put("userId",    profile.getUser().getId());
            response.put("email",     profile.getUser().getEmail());
            response.put("username",  profile.getUsername());
            response.put("fullName",  profile.getFullName());
            response.put("bio",       profile.getBio());
            
            // --- THE FIX IS HERE ---
            response.put("hasPhoto",  profile.getPhoto() != null);
            response.put("photo",     profile.getPhoto()); // <-- Add this line!
            // -----------------------

            response.put("createdAt", profile.getCreatedAt());
            response.put("updatedAt", profile.getUpdatedAt());
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── 2. UPDATE PROFILE ──
    public Object updateProfile(String email, Map<String, String> body) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) return Map.of("success", false, "error", "User not found");

            User user = userOpt.get();
            Optional<Profile> profileOpt = profileRepository.findByUserEmail(email);
            Profile profile;

            // Using standard if/else instead of lambdas
            if (profileOpt.isEmpty()) {
                profile = new Profile();
                profile.setUser(user);
            } else {
                profile = profileOpt.get();
            }

            if (body.containsKey("username")) profile.setUsername(body.get("username"));
            if (body.containsKey("fullName")) profile.setFullName(body.get("fullName"));
            if (body.containsKey("bio"))      profile.setBio(body.get("bio"));

            profile = profileRepository.save(profile);

            String supabaseSyncError = syncProfileToSupabasePublic(user, profile);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",   true);
            response.put("message",   "Profile updated successfully");
            response.put("username",  profile.getUsername());
            response.put("fullName",  profile.getFullName());
            response.put("bio",       profile.getBio());
            response.put("updatedAt", profile.getUpdatedAt());
            response.put("syncedToSupabase", supabaseSyncError == null);
            if (supabaseSyncError != null) response.put("supabaseSyncError", supabaseSyncError);
            
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── 3. UPDATE PASSWORD ──
    public Object updatePassword(String token, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) return Map.of("success", false, "error", "Password cannot be empty");
        if (newPassword.length() < 6) return Map.of("success", false, "error", "Password must be at least 6 characters");

        try {
            String url = config.supabaseUrl + "/auth/v1/user";
            Map<String, String> authBody = new HashMap<>();
            authBody.put("password", newPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(authBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> authUser = (Map<String, Object>) restTemplate.exchange(
                    config.supabaseUrl + "/auth/v1/user", HttpMethod.GET, new HttpEntity<>(headers), Object.class).getBody();

            restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);

            String email = (String) authUser.get("email");
            
            // Using standard if instead of lambdas
            Optional<User> dbUserOpt = userRepository.findByEmail(email);
            if (dbUserOpt.isPresent()) {
                User dbUser = dbUserOpt.get();
                dbUser.setPassword(newPassword);
                userRepository.save(dbUser);
            }

            return Map.of("success", true, "message", "Password updated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── 4. UPLOAD PHOTO ──
    public Object uploadPhoto(String email, byte[] imageBytes) {
        try {
            String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
            if (normalizedEmail.isBlank()) return Map.of("success", false, "error", "Email is required");

            Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
            User user;
            boolean userAutoCreated = false;
            
            if (userOpt.isEmpty()) {
                user = new User();
                user.setEmail(normalizedEmail);
                user.setPassword("external-auth-user");
                user = userRepository.save(user);
                userAutoCreated = true;
            } else {
                user = userOpt.get();
            }

            Optional<Profile> profileOpt = profileRepository.findByUserEmail(normalizedEmail);
            Profile profile;
            
            // Using standard if/else instead of lambdas
            if (profileOpt.isEmpty()) {
                profile = new Profile();
                profile.setUser(user);
            } else {
                profile = profileOpt.get();
            }

            String base64Photo = Base64.getEncoder().encodeToString(imageBytes);
            profile.setPhoto(base64Photo);
            profileRepository.save(profile);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",   true);
            response.put("message",   "Photo uploaded successfully");
            response.put("email",     normalizedEmail);
            response.put("userAutoCreated", userAutoCreated);
            response.put("updatedAt", profile.getUpdatedAt());
            return response;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── HELPER METHODS ──
    private String syncProfileToSupabasePublic(User user, Profile profile) {
        try {
            String email = user.getEmail();
            String userId = resolveSupabaseAuthUserId(email, user.getPassword());
            if (userId == null || userId.isBlank()) userId = user.getId().toString();
            
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
            if (email == null || email.isBlank() || password == null || password.isBlank()) return null;

            String url = config.supabaseUrl + "/auth/v1/token?grant_type=password";
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", config.supabaseAuthKey);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            Map<String, Object> authResponse = (Map<String, Object>) restTemplate.postForObject(url, entity, Object.class);
            
            if (authResponse == null) return null;
            Object userObj = authResponse.get("user");
            if (!(userObj instanceof Map<?, ?> userMap)) return null;

            Object idObj = userMap.get("id");
            return idObj != null ? idObj.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}