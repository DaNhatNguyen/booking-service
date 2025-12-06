package com.example.booking_service.repository;

import com.example.booking_service.entity.FixedBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixedBookingRepository extends JpaRepository<FixedBooking, Long> {
    List<FixedBooking> findByUserId(Long userId);
    
    List<FixedBooking> findByCourtId(Long courtId);
    
    List<FixedBooking> findByStatus(String status);
}













