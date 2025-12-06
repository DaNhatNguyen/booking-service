package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PricePreviewResponse {
    @JsonProperty("total_bookings")
    Integer totalBookings;
    
    @JsonProperty("weekday_count")
    Integer weekdayCount;
    
    @JsonProperty("weekend_count")
    Integer weekendCount;
    
    @JsonProperty("weekday_price_per_session")
    Double weekdayPricePerSession;
    
    @JsonProperty("weekend_price_per_session")
    Double weekendPricePerSession;
    
    @JsonProperty("total_price")
    Double totalPrice;
    
    List<PriceBreakdown> breakdown;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceBreakdown {
        String date;
        
        @JsonProperty("day_type")
        String dayType;
        
        Double price;
    }
}













