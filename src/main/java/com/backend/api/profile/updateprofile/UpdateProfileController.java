package com.backend.api.profile.updateprofile;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UpdateProfileController {

    private final UpdateProfileService updateProfileService;

    public UpdateProfileController(UpdateProfileService updateProfileService) {
        this.updateProfileService = updateProfileService;
    }

    // PATCH http://localhost:8080/api/user/profile?email=user@example.com
    // Body (raw JSON): { "username": "john", "fullName": "John Doe", "bio": "Hello" }
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestParam("email") String email,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(updateProfileService.updateProfile(email, body));
    }
}
