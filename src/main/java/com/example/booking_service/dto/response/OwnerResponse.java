package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class OwnerResponse {
    Long id;
    
    @JsonProperty("full_name")
    String fullName;
    
    String email;
    String phone;
    String role;
}





















