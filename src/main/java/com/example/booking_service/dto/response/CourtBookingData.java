package com.example.booking_service.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class CourtBookingData {
    Long id;
    String name;
    List<BookingData> bookings;
    List<PriceData> prices;
}

