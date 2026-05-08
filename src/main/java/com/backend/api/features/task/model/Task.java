package com.backend.api.features.task.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.backend.api.features.auth.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean urgent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String verificationPhoto;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // --- NEW REVIEW FIELDS ---
    @Column(name = "poster_to_tasker_rating")
    private Integer posterToTaskerRating;
    
    @Column(name = "poster_to_tasker_review", columnDefinition = "TEXT")
    private String posterToTaskerReview;

    @Column(name = "tasker_to_poster_rating")
    private Integer taskerToPosterRating;
    
    @Column(name = "tasker_to_poster_review", columnDefinition = "TEXT")
    private String taskerToPosterReview;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getVerificationPhoto() { return verificationPhoto; }
    public void setVerificationPhoto(String verificationPhoto) { this.verificationPhoto = verificationPhoto; }

    // --- NEW GETTERS AND SETTERS FOR REVIEWS ---
    public Integer getPosterToTaskerRating() { return posterToTaskerRating; }
    public void setPosterToTaskerRating(Integer posterToTaskerRating) { this.posterToTaskerRating = posterToTaskerRating; }
    
    public String getPosterToTaskerReview() { return posterToTaskerReview; }
    public void setPosterToTaskerReview(String posterToTaskerReview) { this.posterToTaskerReview = posterToTaskerReview; }
    
    public Integer getTaskerToPosterRating() { return taskerToPosterRating; }
    public void setTaskerToPosterRating(Integer taskerToPosterRating) { this.taskerToPosterRating = taskerToPosterRating; }
    
    public String getTaskerToPosterReview() { return taskerToPosterReview; }
    public void setTaskerToPosterReview(String taskerToPosterReview) { this.taskerToPosterReview = taskerToPosterReview; }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }
    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }
}