package com.backend.api.features.profile.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.api.features.profile.service.ProfileService;

@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // 1. GET http://localhost:8080/api/user/profile?email=...
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam("email") String email) {
        return ResponseEntity.ok(profileService.getProfile(email));
    }

    // 2. PATCH http://localhost:8080/api/user/profile?email=...
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestParam("email") String email,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(profileService.updateProfile(email, body));
    }

    // 3. PUT http://localhost:8080/api/user/password
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                profileService.updatePassword(token.replace("Bearer ", ""), body.get("password"))
        );
    }

    // 4. POST http://localhost:8080/api/user/upload-photo?email=...
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                profileService.uploadPhoto(email, file.getBytes())
        );
    }
}