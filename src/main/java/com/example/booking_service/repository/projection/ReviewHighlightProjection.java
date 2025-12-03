package com.example.booking_service.repository.projection;

import java.time.LocalDateTime;

public interface ReviewHighlightProjection {
    Long getReviewId();
    String getUserName();
    String getCourtGroupName();
    Integer getRating();
    String getComment();
    LocalDateTime getCreatedAt();
}















