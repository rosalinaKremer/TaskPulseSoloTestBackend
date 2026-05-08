package com.backend.api.features.bid.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.api.features.auth.model.User;
import com.backend.api.features.auth.repository.UserRepository;
import com.backend.api.features.bid.model.Bid;
import com.backend.api.features.bid.repository.BidRepository;
import com.backend.api.features.task.model.Task;
import com.backend.api.features.task.repository.TaskRepository;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public BidService(BidRepository bidRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public Object placeBid(String email, UUID taskId, Map<String, Object> body) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) return Map.of("success", false, "error", "User not found");

            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return Map.of("success", false, "error", "Task not found");

            User bidder = userOpt.get();
            Task task = taskOpt.get();

            // Business Rules
            if (task.getCreator().getId().equals(bidder.getId())) {
                return Map.of("success", false, "error", "You cannot bid on your own task!");
            }
            if (bidRepository.existsByTaskIdAndBidderEmail(taskId, email)) {
                return Map.of("success", false, "error", "You have already placed a bid on this task.");
            }

            Bid bid = new Bid();
            bid.setTask(task);
            bid.setBidder(bidder);
            bid.setAmount(new BigDecimal(body.getOrDefault("amount", task.getPrice().toString()).toString()));
            bid.setCoverLetter(body.getOrDefault("coverLetter", "").toString());

            bidRepository.save(bid);

            return Map.of("success", true, "message", "Bid placed successfully!");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // Get all bids placed by a specific user
    @Transactional(readOnly = true) 
    public List<Map<String, Object>> getMyBids(String email) {
        return bidRepository.findByBidderEmailOrderByCreatedAtDesc(email).stream().map(bid -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", bid.getId());
            dto.put("amount", bid.getAmount());
            dto.put("coverLetter", bid.getCoverLetter());
            dto.put("status", bid.getStatus());
            
            Map<String, Object> taskMap = new LinkedHashMap<>();
            taskMap.put("id", bid.getTask().getId());
            taskMap.put("title", bid.getTask().getTitle());
            taskMap.put("location", bid.getTask().getLocation());
            taskMap.put("price", bid.getTask().getPrice());
            // It is helpful to pass the task status as well so the frontend knows if it's IN_PROGRESS or COMPLETED
            taskMap.put("status", bid.getTask().getStatus()); 
            
            dto.put("task", taskMap);
            return dto;
        }).collect(Collectors.toList());
    }

    // Edit an existing bid
    public Object editBid(String email, UUID bidId, Map<String, Object> body) {
        try {
            Optional<Bid> bidOpt = bidRepository.findById(bidId);
            if (bidOpt.isEmpty()) return Map.of("success", false, "error", "Bid not found");

            Bid bid = bidOpt.get();

            // SECURITY RULE: Only the bidder can edit their own bid
            if (!bid.getBidder().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized: You can only edit your own bids.");
            }

            if (body.containsKey("amount")) bid.setAmount(new BigDecimal(body.get("amount").toString()));
            if (body.containsKey("coverLetter")) bid.setCoverLetter(body.get("coverLetter").toString());

            bidRepository.save(bid);
            return Map.of("success", true, "message", "Bid updated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // Cancel (Delete) an existing bid
    public Object cancelBid(String email, UUID bidId) {
        try {
            Optional<Bid> bidOpt = bidRepository.findById(bidId);
            if (bidOpt.isEmpty()) return Map.of("success", false, "error", "Bid not found");

            Bid bid = bidOpt.get();

            // SECURITY RULE: Only the bidder can delete their own bid
            if (!bid.getBidder().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized: You can only cancel your own bids.");
            }

            bidRepository.delete(bid);
            return Map.of("success", true, "message", "Bid cancelled successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // Let the poster view all bids on their specific task
    @Transactional(readOnly = true)
    public Object getBidsForTask(String email, UUID taskId) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) return Map.of("success", false, "error", "Task not found");
            
            Task task = taskOpt.get();
            if (!task.getCreator().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized: You can only view bids for your own tasks.");
            }

            List<Map<String, Object>> bidsList = bidRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(bid -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", bid.getId());
                dto.put("amount", bid.getAmount());
                dto.put("coverLetter", bid.getCoverLetter());
                dto.put("status", bid.getStatus());
                dto.put("bidderEmail", bid.getBidder().getEmail()); 
                
                // Trustworthiness details for the frontend modal
                // NOTE: If you haven't added getRating() / getReviewsCount() to your User entity, 
                // you can leave these hardcoded as "4.9" and "12" for now. 
                // Otherwise change to: bid.getBidder().getRating()
                dto.put("bidderRating", "4.9"); 
                dto.put("bidderReviewCount", 12); 
                
                return dto;
            }).collect(Collectors.toList());

            return Map.of("success", true, "bids", bidsList);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // Accept a bid (Assigns task, rejects all other bids, updates task status)
    @Transactional
    public Object acceptBid(String email, UUID bidId) {
        try {
            Optional<Bid> bidOpt = bidRepository.findById(bidId);
            if (bidOpt.isEmpty()) return Map.of("success", false, "error", "Bid not found");
            
            Bid acceptedBid = bidOpt.get();
            Task task = acceptedBid.getTask();

            if (!task.getCreator().getEmail().equalsIgnoreCase(email)) {
                return Map.of("success", false, "error", "Unauthorized: You can only accept bids for your own tasks.");
            }

            if (task.getAssignedTo() != null) {
                return Map.of("success", false, "error", "Task is already assigned to someone else.");
            }

            // 1. Assign Task to the Bidder AND update the status
            task.setAssignedTo(acceptedBid.getBidder());
            task.setStatus("IN_PROGRESS");
            taskRepository.save(task);

            // 2. Update Bid Statuses (Accept this one, Reject the rest)
            List<Bid> allBids = bidRepository.findByTaskIdOrderByCreatedAtDesc(task.getId());
            for (Bid b : allBids) {
                if (b.getId().equals(acceptedBid.getId())) {
                    b.setStatus("ACCEPTED");
                } else {
                    b.setStatus("REJECTED");
                }
            }
            bidRepository.saveAll(allBids);

            return Map.of("success", true, "message", "Bid accepted! Task is now assigned and in progress.");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}