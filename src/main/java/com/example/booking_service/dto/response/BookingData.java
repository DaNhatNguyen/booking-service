package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BookingData {
    Long id;

    @JsonProperty("booking_date")
    String bookingDate;

    @JsonProperty("start_time")
    String startTime;

    @JsonProperty("end_time")
    String endTime;

    @JsonProperty("total_price")
    Double totalPrice;
}




