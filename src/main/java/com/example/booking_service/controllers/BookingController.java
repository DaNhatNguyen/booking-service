package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateBookingRequest;
import com.example.booking_service.dto.response.BookingByDateResponse;
import com.example.booking_service.dto.response.CreateBookingResponse;
import com.example.booking_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;

    @GetMapping("/{courtGroupId}/data")
    public ApiResponse<BookingByDateResponse> getBookingDataByDate(
            @PathVariable Long courtGroupId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") String date) {
        LocalDate bookingDate = LocalDate.parse(date);
        BookingByDateResponse response = bookingService.getBookingDataByDate(courtGroupId, bookingDate);
        return ApiResponse.<BookingByDateResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/confirmation")
    public ApiResponse<CreateBookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
        CreateBookingResponse response = bookingService.createBooking(request);
        return ApiResponse.<CreateBookingResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/{bookingId}/confirm")
    public ApiResponse<CreateBookingResponse> confirmBooking(@PathVariable Long bookingId) {
        CreateBookingResponse response = bookingService.confirmBooking(bookingId);
        return ApiResponse.<CreateBookingResponse>builder()
                .result(response)
                .build();
    }
}