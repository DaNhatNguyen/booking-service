package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.ConflictCheckRequest;
import com.example.booking_service.dto.request.CreateFixedBookingRequest;
import com.example.booking_service.dto.request.PricePreviewRequest;
import com.example.booking_service.dto.response.ConflictCheckResponse;
import com.example.booking_service.dto.response.FixedBookingResponse;
import com.example.booking_service.dto.response.PricePreviewResponse;
import com.example.booking_service.service.FixedBookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fixed-bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FixedBookingController {

    FixedBookingService fixedBookingService;

    /**
     * Preview price for fixed booking
     * POST /fixed-bookings/preview-price
     */
    @PostMapping("/preview-price")
    public ApiResponse<PricePreviewResponse> previewPrice(@RequestBody PricePreviewRequest request) {
        PricePreviewResponse response = fixedBookingService.calculatePrice(request);
        return ApiResponse.<PricePreviewResponse>builder()
                .code(1000)
                .result(response)
                .build();
    }

    /**
     * Check for conflicts with existing bookings
     * POST /fixed-bookings/check-conflicts
     */
    @PostMapping("/check-conflicts")
    public ApiResponse<ConflictCheckResponse> checkConflicts(@RequestBody ConflictCheckRequest request) {
        ConflictCheckResponse response = fixedBookingService.checkConflicts(request);
        return ApiResponse.<ConflictCheckResponse>builder()
                .code(1000)
                .result(response)
                .build();
    }

    /**
     * Create fixed booking and generate individual bookings
     * POST /fixed-bookings
     */
    @PostMapping
    public ApiResponse<FixedBookingResponse> createFixedBooking(@RequestBody CreateFixedBookingRequest request) {
        FixedBookingResponse response = fixedBookingService.createFixedBooking(request);
        return ApiResponse.<FixedBookingResponse>builder()
                .code(1000)
                .message("Đã tạo đặt lịch cố định thành công")
                .result(response)
                .build();
    }
}

