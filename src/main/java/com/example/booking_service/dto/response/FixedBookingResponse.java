package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FixedBookingResponse {
    Long id;
    
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("court_id")
    Long courtId;
    
    @JsonProperty("start_date")
    String startDate;
    
    @JsonProperty("end_date")
    String endDate;
    
    @JsonProperty("days_of_week")
    String daysOfWeek;
    
    @JsonProperty("start_time")
    String startTime;
    
    @JsonProperty("end_time")
    String endTime;
    
    String status;
    
    @JsonProperty("created_bookings_count")
    Integer createdBookingsCount;
}













