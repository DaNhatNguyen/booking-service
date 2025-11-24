package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCourtRequest {
    
    @JsonProperty("court_group_id")
    Long courtGroupId;
    
    String name;
    
    @JsonProperty("is_active")
    Integer isActive; // 1 = available, 0 = locked
}






