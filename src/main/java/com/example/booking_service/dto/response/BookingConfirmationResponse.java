package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class BookingConfirmationResponse {
    @JsonProperty("court_group_id")
    Long courtGroupId;

    @JsonProperty("court_group_name")
    String courtGroupName;

    @JsonProperty("full_address")
    String fullAddress;

    @JsonProperty("booking_date")
    String bookingDate;

    @JsonProperty("court_name")
    String courtName;

    @JsonProperty("time_slots")
    List<TimeSlotInfo> timeSlots;

    @JsonProperty("total_price")
    Double totalPrice;
}

