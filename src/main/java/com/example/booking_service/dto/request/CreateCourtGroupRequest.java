package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCourtGroupRequest {
    
    @JsonProperty("owner_id")
    Long ownerId;
    
    @JsonProperty("field_name")
    String fieldName;
    
    @JsonProperty("field_type")
    String fieldType;
    
    String address;
    
    String district;
    
    String province;
    
    String phone;
    
    @JsonProperty("open_time")
    String openTime; // HH:mm format
    
    @JsonProperty("close_time")
    String closeTime; // HH:mm format
    
    @JsonProperty("court_number")
    Integer courtNumber;
    
    String description;
}








