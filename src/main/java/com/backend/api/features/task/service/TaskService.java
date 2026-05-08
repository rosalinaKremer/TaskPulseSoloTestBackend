package com.backend.api.features.task.service;

import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backend.api.features.auth.model.User;
import com.backend.api.features.auth.repository.UserRepository;
import com.backend.api.features.task.model.Task;
import com.backend.api.features.task.repository.TaskRepository;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // ── Helper: Map Task Entity to DTO ──
    private Map<String, Object> mapTaskToDto(Task task) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", task.getId());
        dto.put("title", task.getTitle());
        dto.put("category", task.getCategory());
        dto.put("location", task.getLocation());
        dto.put("price", task.getPrice());
        dto.put("description", task.getDescription());
        dto.put("urgent", task.isUrgent());
        dto.put("status", task.getStatus());

        // Formatting dates for the frontend input (Date-only)
        dto.put("startDate", task.getStartDate() != null ? task.getStartDate().toLocalDate().toString() : null);
        dto.put("endDate", task.getEndDate() != null ? task.getEndDate().toLocalDate().toString() : null);

        // Verification & Reviews
        dto.put("verificationPhoto", task.getVerificationPhoto());
        dto.put("posterToTaskerRating", task.getPosterToTaskerRating());
        dto.put("posterToTaskerReview", task.getPosterToTaskerReview());
        dto.put("taskerToPosterRating", task.getTaskerToPosterRating());
        dto.put("taskerToPosterReview", task.getTaskerToPosterReview());

        dto.put("creatorEmail", task.getCreator() != null ? task.getCreator().getEmail() : null);
        dto.put("assignedToEmail", task.getAssignedTo() != null ? task.getAssignedTo().getEmail() : null);
        
        return dto;
    }

    // ── Get Available Tasks (For the "Find Work" Feed) ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableTasks() {
        return taskRepository.findByAssignedToIsNullOrderByCreatedAtDesc()
                .stream()
                .map(this::mapTaskToDto)
                .collect(Collectors.toList());
    }

    // ── Get My Posted Tasks ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyPosts(String email) {
        return taskRepository.findByCreatorEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::mapTaskToDto)
                .collect(Collectors.toList());
    }

    // ── Create a New Task ──
    @Transactional
    public Object createTask(String email, Map<String, Object> body) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) return Map.of("success", false, "error", "User not found");

            Task task = new Task();
            task.setCreator(userOpt.get());
            task.setTitle(body.getOrDefault("title", "").toString());
            task.setCategory(body.getOrDefault("category", "All Tasks").toString());
            task.setLocation(body.getOrDefault("location", "").toString());
            task.setPrice(new java.math.BigDecimal(body.getOrDefault("price", "0").toString()));
            task.setDescription(body.getOrDefault("description", "").toString());
            task.setUrgent(Boolean.parseBoolean(body.getOrDefault("urgent", "false").toString()));
            task.setStatus("OPEN");

            if (body.get("startDate") != null && !body.get("startDate").toString().isEmpty()) {
                task.setStartDate(LocalDate.parse(body.get("startDate").toString()).atStartOfDay());
            }
            if (body.get("endDate") != null && !body.get("endDate").toString().isEmpty()) {
                task.setEndDate(LocalDate.parse(body.get("endDate").toString()).atTime(23, 59, 59));
            }

            taskRepository.save(task);
            return Map.of("success", true, "message", "Task posted successfully!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── Edit an Existing Task ──
    @Transactional
    public Object editTask(String email, UUID taskId, Map<String, Object> body) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new Exception("Task not found"));
            if (!task.getCreator().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized.");
            }

            if (body.containsKey("title")) task.setTitle(body.get("title").toString());
            if (body.containsKey("category")) task.setCategory(body.get("category").toString());
            if (body.containsKey("location")) task.setLocation(body.get("location").toString());
            if (body.containsKey("price")) task.setPrice(new java.math.BigDecimal(body.get("price").toString()));
            if (body.containsKey("description")) task.setDescription(body.get("description").toString());
            if (body.containsKey("urgent")) task.setUrgent(Boolean.parseBoolean(body.get("urgent").toString()));

            if (body.containsKey("startDate") && !body.get("startDate").toString().isEmpty()) {
                task.setStartDate(LocalDate.parse(body.get("startDate").toString()).atStartOfDay());
            }
            if (body.containsKey("endDate") && !body.get("endDate").toString().isEmpty()) {
                task.setEndDate(LocalDate.parse(body.get("endDate").toString()).atTime(23, 59, 59));
            }

            taskRepository.save(task);
            return Map.of("success", true, "message", "Task updated successfully!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── Tasker Submits Proof of Work ──
    @Transactional
    public Object verifyFinishTask(UUID taskId, String email, MultipartFile photo) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new Exception("Task not found"));
            
            if (task.getAssignedTo() == null || !task.getAssignedTo().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized: You are not assigned to this task.");
            }

            if (photo != null && !photo.isEmpty()) {
                String base64Photo = Base64.getEncoder().encodeToString(photo.getBytes());
                task.setVerificationPhoto(base64Photo);
            } else {
                return Map.of("success", false, "error", "Photo is required for verification.");
            }

            task.setStatus("REVIEW");
            taskRepository.save(task);
            return Map.of("success", true, "message", "Work submitted for review!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── Poster Confirms Completion & Leaves Review ──
    @Transactional
    public Object confirmCompletion(UUID taskId, String email, Integer rating, String reviewText) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new Exception("Task not found"));
            
            if (!task.getCreator().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized.");
            }

            task.setStatus("COMPLETED");
            
            if (rating != null) task.setPosterToTaskerRating(rating);
            if (reviewText != null && !reviewText.isEmpty()) task.setPosterToTaskerReview(reviewText);

            taskRepository.save(task);
            return Map.of("success", true, "message", "Payment released and review submitted!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ── Tasker Reviews Poster ──
    @Transactional
    public Object ratePoster(UUID taskId, String email, Integer rating, String reviewText) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new Exception("Task not found"));
            
            if (task.getAssignedTo() == null || !task.getAssignedTo().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized.");
            }
            if (!"COMPLETED".equals(task.getStatus())) {
                return Map.of("success", false, "error", "Task must be completed before reviewing.");
            }

            task.setTaskerToPosterRating(rating);
            task.setTaskerToPosterReview(reviewText);
            
            taskRepository.save(task);
            return Map.of("success", true, "message", "Review submitted for the Poster!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserReviews(String email) {
        
        // Reviews AS TASKER (The Poster reviewed this Tasker)
        List<Map<String, Object>> asTasker = taskRepository.findByAssignedTo_EmailAndStatus(email, "COMPLETED")
                .stream()
                .filter(t -> t.getPosterToTaskerRating() != null)
                .map(t -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", t.getId() + "_tasker");
                    r.put("name", t.getCreator().getEmail());
                    r.put("date", t.getUpdatedAt().toLocalDate().toString());
                    r.put("rating", t.getPosterToTaskerRating());
                    r.put("text", t.getPosterToTaskerReview());
                    r.put("taskTitle", t.getTitle());
                    return r;
                }).collect(Collectors.toList());

        // Reviews AS POSTER (The Tasker reviewed this Poster)
        List<Map<String, Object>> asPoster = taskRepository.findByCreator_EmailAndStatus(email, "COMPLETED")
                .stream()
                .filter(t -> t.getTaskerToPosterRating() != null)
                .map(t -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", t.getId() + "_poster");
                    r.put("name", t.getAssignedTo().getEmail());
                    r.put("date", t.getUpdatedAt().toLocalDate().toString());
                    r.put("rating", t.getTaskerToPosterRating());
                    r.put("text", t.getTaskerToPosterReview());
                    r.put("taskTitle", t.getTitle());
                    return r;
                }).collect(Collectors.toList());

        return Map.of("asTasker", asTasker, "asPoster", asPoster);
    }
}