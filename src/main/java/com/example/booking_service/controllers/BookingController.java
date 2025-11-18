package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.BookingConfirmationResponse;
import com.example.booking_service.dto.response.BookingDataResponse;
import com.example.booking_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;

    @GetMapping("/court-group/{courtGroupId}/data")
    public ApiResponse<BookingDataResponse> getBookingData(
            @PathVariable Long courtGroupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.<BookingDataResponse>builder()
                .result(bookingService.getBookingData(courtGroupId, date))
                .build();
    }

    @PostMapping("/confirmation")
    public ApiResponse<BookingConfirmationResponse> getBookingConfirmation(
            @RequestBody BookingConfirmationRequest request) {
        BookingConfirmationResponse response = bookingService.getBookingConfirmation(
                request.getCourtGroupId(),
                request.getCourtId(),
                request.getDate(),
                request.getSelectedSlots()
        );
        return ApiResponse.<BookingConfirmationResponse>builder()
                .result(response)
                .build();
    }

    public static class BookingConfirmationRequest {
        private Long courtGroupId;
        private Long courtId;
        private LocalDate date;
        private List<BookingService.TimeSlotSelection> selectedSlots;

        // Getters and setters
        public Long getCourtGroupId() { return courtGroupId; }
        public void setCourtGroupId(Long courtGroupId) { this.courtGroupId = courtGroupId; }

        public Long getCourtId() { return courtId; }
        public void setCourtId(Long courtId) { this.courtId = courtId; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public List<BookingService.TimeSlotSelection> getSelectedSlots() { return selectedSlots; }
        public void setSelectedSlots(List<BookingService.TimeSlotSelection> selectedSlots) {
            this.selectedSlots = selectedSlots;
        }
    }
}