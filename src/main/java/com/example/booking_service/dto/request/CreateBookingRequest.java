package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBookingRequest {
    
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("court_id")
    Long courtId;
    
    @JsonProperty("booking_date")
    String bookingDate; // yyyy-MM-dd
    
    @JsonProperty("court_name")
    String courtName;
    
    @JsonProperty("full_address")
    String fullAddress;
    
    String status;
    
    @JsonProperty("time_slots")
    List<TimeSlot> timeSlots;
    
    @JsonProperty("total_price")
    Double totalPrice;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        @JsonProperty("start_time")
        String startTime; // HH:mm
        
        @JsonProperty("end_time")
        String endTime; // HH:mm
    }
}

