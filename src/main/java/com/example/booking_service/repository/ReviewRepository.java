package com.example.booking_service.repository;

import com.example.booking_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourtGroupId(Long courtGroupId);

    Optional<Review> findByBookingId(Long bookingId);
}


