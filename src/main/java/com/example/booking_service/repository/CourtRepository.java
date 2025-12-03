package com.example.booking_service.repository;

import com.example.booking_service.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByCourtGroupId(Long courtGroupId);
    
    @Query("SELECT c FROM Court c WHERE c.courtGroupId = :courtGroupId " +
           "AND (c.isActive = 1 OR c.isActive IS NULL)")
    List<Court> findActiveCourtsByCourtGroupId(@Param("courtGroupId") Long courtGroupId);
    
    List<Court> findByCourtGroupIdIn(List<Long> courtGroupIds);
}