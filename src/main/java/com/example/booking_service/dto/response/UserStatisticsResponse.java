package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserStatisticsResponse {
    @JsonProperty("total_bookings")
    Long totalBookings;
    
    @JsonProperty("total_spent")
    Double totalSpent;
    
    @JsonProperty("favorite_courts")
    Long favoriteCourts;
    
    @JsonProperty("total_reviews")
    Long totalReviews;
}


















