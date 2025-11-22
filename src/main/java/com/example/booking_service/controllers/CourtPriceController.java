package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateCourtPriceRequest;
import com.example.booking_service.dto.response.CourtPriceResponse;
import com.example.booking_service.service.CourtPriceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/court-prices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtPriceController {

    CourtPriceService courtPriceService;

    @GetMapping("/court-group/{courtGroupId}")
    public ApiResponse<List<CourtPriceResponse>> getCourtPricesByCourtGroupId(@PathVariable Long courtGroupId) {
        return ApiResponse.<List<CourtPriceResponse>>builder()
                .result(courtPriceService.getCourtPricesByCourtGroupId(courtGroupId))
                .build();
    }

    @PostMapping
    public ApiResponse<CourtPriceResponse> createOrUpdateCourtPrice(@RequestBody CreateCourtPriceRequest request) {
        CourtPriceResponse response = courtPriceService.createOrUpdateCourtPrice(request);
        return ApiResponse.<CourtPriceResponse>builder()
                .message("Đã cập nhật giá thành công")
                .result(response)
                .build();
    }
}

