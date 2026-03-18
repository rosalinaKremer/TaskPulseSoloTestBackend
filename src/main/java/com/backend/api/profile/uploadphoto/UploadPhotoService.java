package com.backend.api.profile.uploadphoto;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.backend.api.entity.Profile;
import com.backend.api.entity.User;
import com.backend.api.repository.ProfileRepository;
import com.backend.api.repository.UserRepository;

@Service
public class UploadPhotoService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UploadPhotoService(ProfileRepository profileRepository,
                              UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public Object uploadPhoto(String email, byte[] imageBytes) {
        try {
            String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
            if (normalizedEmail.isBlank()) {
                return Map.of("success", false, "error", "Email is required");
            }

            // Step 1: Find user (auto-create local row if missing)
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

            // Step 2: Find profile — auto-create if missing
            Optional<Profile> profileOpt = profileRepository.findByUserEmail(normalizedEmail);
            Profile profile;

            if (profileOpt.isEmpty()) {
                profile = new Profile();
                profile.setUser(user);
            } else {
                profile = profileOpt.get();
            }

            // Step 3: Convert image to BLOB / BYTES (Base64)
            String base64Photo = Base64.getEncoder().encodeToString(imageBytes);
            profile.setPhoto(base64Photo);

            // Step 4: Save via JPA
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
}
