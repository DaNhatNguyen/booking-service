package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourtPriceDTO {
    Long id;
    
    @JsonProperty("timeSlotId")
    Long timeSlotId;
    
    @JsonProperty("startTime")
    String startTime;
    
    @JsonProperty("endTime")
    String endTime;
    
    @JsonProperty("dayType")
    String dayType; // "WEEKDAY" or "WEEKEND"
    
    Double price;
}

