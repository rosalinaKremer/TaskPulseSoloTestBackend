package com.backend.api.profile.updatepassword;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UpdatePasswordController {

    private final UpdatePasswordService updatePasswordService;

    public UpdatePasswordController(UpdatePasswordService updatePasswordService) {
        this.updatePasswordService = updatePasswordService;
    }

    // PUT http://localhost:8080/api/user/password
    // Header: Authorization: Bearer <access_token>
    // Body (raw JSON): { "password": "newpassword123" }
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                updatePasswordService.updatePassword(
                        token.replace("Bearer ", ""), body.get("password"))
        );
    }
}
