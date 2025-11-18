// src/main/java/com/booking/dto/court/CourtAvailabilityResponse.java
package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtAvailabilityResponse {
    private List<CourtDetail> bookingCourts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtDetail {
        private Long id;
        private String name;

        private List<BookingBlock> bookings;

        private List<TimeSlotPrice> prices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingBlock {
        private Long id; // booking_id
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate bookingDate;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
        private Double totalPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotPrice {
        private Long timeSlotId;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime startTime;
        @JsonFormat(pattern = "HH:mm")
        private LocalTime endTime;
        private Double price;
    }
}