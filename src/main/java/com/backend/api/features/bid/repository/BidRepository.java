package com.backend.api.features.bid.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.api.features.bid.model.Bid;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    
    List<Bid> findByTaskIdOrderByCreatedAtDesc(UUID taskId);
    
    boolean existsByTaskIdAndBidderEmail(UUID taskId, String email);
    
    List<Bid> findByBidderEmailOrderByCreatedAtDesc(String email);
}