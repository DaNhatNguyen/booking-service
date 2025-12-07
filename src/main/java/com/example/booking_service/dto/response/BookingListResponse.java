package com.example.booking_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingListResponse {
    List<BookingDetailResponse> bookings;
    PaginationResponse pagination;
}

























