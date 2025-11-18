package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class CourtGroupResponse {

    @JsonProperty("_id")
    String id;

    String name;
    String type;
    String address;
    String district;
    String province;

    String phoneNumber;

    List<String> images;

    String openTime;
    String closeTime;

    Double rating;

    String description;
}



