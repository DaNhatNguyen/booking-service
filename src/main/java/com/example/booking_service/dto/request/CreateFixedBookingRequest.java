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
public class CreateFixedBookingRequest {
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("court_id")
    Long courtId;
    
    @JsonProperty("start_date")
    String startDate; // "yyyy-MM-dd"
    
    @JsonProperty("end_date")
    String endDate; // "yyyy-MM-dd"
    
    @JsonProperty("days_of_week")
    List<Integer> daysOfWeek; // [1, 3, 5]
    
    @JsonProperty("start_time")
    String startTime; // "HH:mm"
    
    @JsonProperty("end_time")
    String endTime; // "HH:mm"
}















