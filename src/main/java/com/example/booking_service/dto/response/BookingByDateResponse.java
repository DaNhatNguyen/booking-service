package com.example.booking_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingByDateResponse {
    List<BookingCourtData> bookingCourts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BookingCourtData {
        Long id;
        String name;
        List<BookingInfo> bookings;
        List<PriceInfo> prices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BookingInfo {
        Long id;
        String bookingDate;
        String startTime;
        String endTime;
        Double totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceInfo {
        Long timeSlotId;
        String startTime;
        String endTime;
        Double price;
    }
}

