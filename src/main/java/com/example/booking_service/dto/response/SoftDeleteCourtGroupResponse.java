package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SoftDeleteCourtGroupResponse {
    
    @JsonProperty("_id")
    String id;
    
    @JsonProperty("is_deleted")
    Integer isDeleted;
}

