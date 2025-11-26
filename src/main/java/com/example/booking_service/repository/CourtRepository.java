package com.example.booking_service.repository;

import com.example.booking_service.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByCourtGroupId(Long courtGroupId);
    
    List<Court> findByCourtGroupIdIn(List<Long> courtGroupIds);
}