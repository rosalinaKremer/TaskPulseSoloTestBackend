package com.backend.api.profile.getprofile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.backend.api.entity.Profile;
import com.backend.api.entity.User;
import com.backend.api.repository.ProfileRepository;
import com.backend.api.repository.UserRepository;

@Service
public class GetProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public GetProfileService(ProfileRepository profileRepository,
                             UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public Object getProfile(String email) {
        try {
            // Step 1: Find user by email
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return Map.of("success", false, "error", "User not found with email: " + email);
            }

            User user = userOpt.get();

            // Step 2: Find profile — auto-create if missing
            Optional<Profile> profileOpt = profileRepository.findByUserEmail(email);
            Profile profile;

            if (profileOpt.isEmpty()) {
                // Profile row missing — create it now
                profile = new Profile();
                profile.setUser(user);
                profile = profileRepository.save(profile);
            } else {
                profile = profileOpt.get();
            }

            // Step 3: Return profile data
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success",   true);
            response.put("id",        profile.getId());
            response.put("userId",    profile.getUser().getId());
            response.put("email",     profile.getUser().getEmail());
            response.put("username",  profile.getUsername());
            response.put("fullName",  profile.getFullName());
            response.put("bio",       profile.getBio());
            response.put("hasPhoto",  profile.getPhoto() != null);
            response.put("createdAt", profile.getCreatedAt());
            response.put("updatedAt", profile.getUpdatedAt());
            return response;

        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}