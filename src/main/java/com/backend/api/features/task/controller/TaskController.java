package com.backend.api.features.task.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.api.features.task.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ── Get Feed ──
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableTasks() {
        return ResponseEntity.ok(taskService.getAvailableTasks());
    }

    // ── Get My Posted Tasks ──
    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPosts(@RequestParam("email") String email) {
        return ResponseEntity.ok(taskService.getMyPosts(email));
    }

    // ── Create Task ──
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestParam("email") String email, 
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(taskService.createTask(email, body));
    }

    // ── Edit Task ──
    @PutMapping("/{taskId}")
    public ResponseEntity<?> editTask(
            @PathVariable UUID taskId,
            @RequestParam("email") String email, 
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(taskService.editTask(email, taskId, body));
    }

    // ── Tasker: Upload Verification Photo ──
    @PostMapping("/{taskId}/verify-finish")
    public ResponseEntity<?> verifyFinishTask(
            @PathVariable UUID taskId, 
            @RequestParam("email") String email, 
            @RequestParam("photo") MultipartFile photo) {
        return ResponseEntity.ok(taskService.verifyFinishTask(taskId, email, photo));
    }

    // ── Poster: Confirm Completion & Rate Tasker ──
    @PostMapping("/{taskId}/confirm")
    public ResponseEntity<?> confirmCompletion(
            @PathVariable UUID taskId, 
            @RequestParam("email") String email,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "reviewText", required = false) String reviewText) {
        return ResponseEntity.ok(taskService.confirmCompletion(taskId, email, rating, reviewText));
    }

    // ── Tasker: Rate Poster ──
    @PostMapping("/{taskId}/rate-poster")
    public ResponseEntity<?> ratePoster(
            @PathVariable UUID taskId, 
            @RequestParam("email") String email,
            @RequestParam("rating") Integer rating,
            @RequestParam("reviewText") String reviewText) {
        return ResponseEntity.ok(taskService.ratePoster(taskId, email, rating, reviewText));
    }

    @GetMapping("/user-reviews")
    public ResponseEntity<?> getUserReviews(@RequestParam("email") String email) {
        return ResponseEntity.ok(taskService.getUserReviews(email));
    }
}