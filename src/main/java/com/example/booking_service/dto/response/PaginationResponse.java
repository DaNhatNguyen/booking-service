package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaginationResponse {
    Long total;
    Integer page;
    Integer limit;
    
    @JsonProperty("totalPages")
    Integer totalPages;
}





