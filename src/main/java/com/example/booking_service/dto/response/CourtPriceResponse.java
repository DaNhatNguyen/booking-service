package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourtPriceResponse {
    Long id;
    
    @JsonProperty("court_group_id")
    Long courtGroupId;
    
    @JsonProperty("time_slot_id")
    Long timeSlotId;
    
    @JsonProperty("day_type")
    String dayType;
    
    Double price;
    
    @JsonProperty("effective_date")
    String effectiveDate;
}

