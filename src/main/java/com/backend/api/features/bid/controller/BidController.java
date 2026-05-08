package com.backend.api.features.bid.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.api.features.bid.service.BidService;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/task/{taskId}")
    public ResponseEntity<?> placeBid(
            @RequestParam("email") String email,
            @PathVariable("taskId") UUID taskId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(bidService.placeBid(email, taskId, body));
    }

    @GetMapping("/my-bids")
    public ResponseEntity<?> getMyBids(@RequestParam("email") String email) {
        try {
            return ResponseEntity.ok(bidService.getMyBids(email));
        } catch (Exception e) {
            // CRASH LOGGER: If a 500 error happens, print the exact reason to the VS Code Terminal!
            System.err.println("CRASH IN GET MY BIDS: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{bidId}")
    public ResponseEntity<?> editBid(
            @RequestParam("email") String email, 
            @PathVariable("bidId") UUID bidId, 
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(bidService.editBid(email, bidId, body));
    }

    @DeleteMapping("/{bidId}")
    public ResponseEntity<?> cancelBid(
            @RequestParam("email") String email, 
            @PathVariable("bidId") UUID bidId) {
        return ResponseEntity.ok(bidService.cancelBid(email, bidId));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getBidsForTask(
            @RequestParam("email") String email, 
            @PathVariable("taskId") UUID taskId) {
        return ResponseEntity.ok(bidService.getBidsForTask(email, taskId));
    }

    @PostMapping("/{bidId}/accept")
    public ResponseEntity<?> acceptBid(
            @RequestParam("email") String email, 
            @PathVariable("bidId") UUID bidId) {
        return ResponseEntity.ok(bidService.acceptBid(email, bidId));
    }
}