package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBookingResponse {
    
    @JsonProperty("booking_id")
    Long bookingId;
    
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("court_id")
    Long courtId;
    
    @JsonProperty("booking_date")
    String bookingDate;
    
    @JsonProperty("start_time")
    String startTime;
    
    @JsonProperty("end_time")
    String endTime;
    
    String status;
    
    @JsonProperty("total_price")
    Double totalPrice;
    
    String address;
    
    String message;
}

