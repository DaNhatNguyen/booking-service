package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponse {
    Long id;
    
    @JsonProperty("user_id")
    Long userId;
    
    @JsonProperty("user_name")
    String userName;
    
    @JsonProperty("user_email")
    String userEmail;
    
    @JsonProperty("user_phone")
    String userPhone;
    
    @JsonProperty("court_id")
    Long courtId;
    
    @JsonProperty("court_name")
    String courtName;
    
    @JsonProperty("court_group_id")
    Long courtGroupId;
    
    @JsonProperty("court_group_name")
    String courtGroupName;
    
    @JsonProperty("court_group_type")
    String courtGroupType;
    
    @JsonProperty("court_group_address")
    String courtGroupAddress;
    
    @JsonProperty("booking_date")
    String bookingDate;
    
    @JsonProperty("start_time")
    String startTime;
    
    @JsonProperty("end_time")
    String endTime;
    
    String status;
    
    Double price;
    
    @JsonProperty("created_at")
    String createdAt;
    
    String address;
}


