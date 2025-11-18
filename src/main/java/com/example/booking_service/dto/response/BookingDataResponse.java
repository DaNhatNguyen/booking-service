package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class BookingDataResponse {
    @JsonProperty("id")
    Long id;

    String name;

    @JsonProperty("default_price")
    Double defaultPrice;

    @JsonProperty("booking_courts")
    List<BookingCourtResponse> bookingCourts;
}