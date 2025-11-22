package com.example.booking_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long courtGroupId;
    private Long bookingId;
    private Integer rating;
    private String comment;
    private String createdAt;
}


