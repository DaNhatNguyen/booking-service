package com.example.booking_service.repository;

import com.example.booking_service.entity.Review;
import com.example.booking_service.repository.projection.ReviewHighlightProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourtGroupId(Long courtGroupId);

    Optional<Review> findByBookingId(Long bookingId);
    
    long countByUserId(Long userId);
    
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r")
    Double findAverageRating();
    
    @Query("""
            SELECT r.id AS reviewId,
                   u.fullName AS userName,
                   cg.name AS courtGroupName,
                   r.rating AS rating,
                   r.comment AS comment,
                   r.createdAt AS createdAt
            FROM Review r
            JOIN User u ON r.userId = u.id
            JOIN CourtGroup cg ON r.courtGroupId = cg.id
            ORDER BY r.createdAt DESC
            """)
    Page<ReviewHighlightProjection> findReviewHighlights(Pageable pageable);
    
    // Owner dashboard queries
    @Query("SELECT r FROM Review r WHERE r.courtGroupId IN :courtGroupIds")
    List<Review> findByCourtGroupIdIn(@Param("courtGroupIds") List<Long> courtGroupIds);
    
    @Query("SELECT r FROM Review r WHERE r.courtGroupId IN :courtGroupIds " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findByCourtGroupIdInOrderByCreatedAtDesc(
            @Param("courtGroupIds") List<Long> courtGroupIds,
            Pageable pageable);
}


