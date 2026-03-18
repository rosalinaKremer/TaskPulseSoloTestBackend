package com.backend.api.profile.getprofile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class GetProfileController {

    private final GetProfileService getProfileService;

    public GetProfileController(GetProfileService getProfileService) {
        this.getProfileService = getProfileService;
    }

    // GET http://localhost:8080/api/user/profile?email=user@example.com
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam("email") String email) {
        return ResponseEntity.ok(getProfileService.getProfile(email));
    }
}
