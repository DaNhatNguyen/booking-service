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
public class ConflictCheckResponse {
    List<ConflictBooking> conflicts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ConflictBooking {
        @JsonProperty("booking_date")
        String bookingDate;
        
        @JsonProperty("start_time")
        String startTime;
        
        @JsonProperty("end_time")
        String endTime;
        
        @JsonProperty("court_name")
        String courtName;
    }
}










