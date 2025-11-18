package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
public class CourtResponse {

    @JsonProperty("id")
    Long id;

    @JsonProperty("court_group_id")
    Long courtGroupId;

    String name;

    @JsonProperty("is_active")
    Integer isActive;

    @JsonProperty("created_at")
    LocalDateTime createdAt;
}






