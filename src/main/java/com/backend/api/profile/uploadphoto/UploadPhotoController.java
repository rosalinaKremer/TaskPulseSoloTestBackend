package com.backend.api.profile.uploadphoto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UploadPhotoController {

    private final UploadPhotoService uploadPhotoService;

    public UploadPhotoController(UploadPhotoService uploadPhotoService) {
        this.uploadPhotoService = uploadPhotoService;
    }

    // POST http://localhost:8080/api/user/upload-photo?email=user@example.com
    // Body: form-data → key=photo, value=<image file .jpg or .png>
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(
            @RequestParam("email") String email,
            @RequestParam("photo") MultipartFile file) throws Exception {
        return ResponseEntity.ok(
                uploadPhotoService.uploadPhoto(email, file.getBytes())
        );
    }
}
