package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CourtGroupListResponse {
    Long id;
    
    @JsonProperty("_id")
    String stringId;
    
    String name;
    String type;
    String address;
    String district;
    String province;
    String phone;
    String description;
    String image;
    Double rating;
    
    @JsonProperty("open_time")
    String openTime;
    
    @JsonProperty("close_time")
    String closeTime;
    
    String status;
    
    @JsonProperty("created_at")
    String createdAt;
    
    @JsonProperty("owner_id")
    Long ownerId;
    
    OwnerResponse owner;
}




