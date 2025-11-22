package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserBookingHistoryResponse {
    
    @JsonProperty("_id")
    Long id;
    
    String date;
    
    @JsonProperty("timeSlot")
    TimeSlotInfo timeSlot;
    
    String status;
    
    @JsonProperty("courtName")
    String courtName;
    
    @JsonProperty("courtGroupName")
    String courtGroupName;
    
    String address;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TimeSlotInfo {
        @JsonProperty("startTime")
        String startTime;
        
        @JsonProperty("endTime")
        String endTime;
    }
}

