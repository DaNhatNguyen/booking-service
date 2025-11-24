package com.example.booking_service.dto.request;

import lombok.Data;

@Data
public class CreateReviewRequest {

    private Long bookingId;
    private Long courtGroupId;
    private Integer rating; // 1-5
    private String comment;
}


