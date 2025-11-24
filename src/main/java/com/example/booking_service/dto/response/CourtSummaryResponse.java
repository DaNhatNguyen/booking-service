package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CourtSummaryResponse {
    Long id;
    String name;
    String status;
    
    @JsonProperty("is_active")
    Integer isActive;
}




