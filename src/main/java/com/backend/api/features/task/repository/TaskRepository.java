package com.backend.api.features.task.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.api.features.task.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    // Finds tasks that are not yet assigned to any tasker
    List<Task> findByAssignedToIsNullOrderByCreatedAtDesc();
    
    // Finds all tasks created by a specific user email
    List<Task> findByCreatorEmailOrderByCreatedAtDesc(String email);

    List<Task> findByAssignedTo_EmailAndStatus(String email, String status);
    
    List<Task> findByCreator_EmailAndStatus(String email, String status);
}