package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SoftDeleteCourtGroupRequest {
    
    @JsonProperty("is_deleted")
    Integer isDeleted; // 1 = deleted, 0 = not deleted
}

