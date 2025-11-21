package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.CourtResponse;
import com.example.booking_service.service.CourtService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtController {

    CourtService courtService;

    @GetMapping("/{id}")
    public ApiResponse<CourtResponse> getCourtById(@PathVariable Long id) {
        return ApiResponse.<CourtResponse>builder()
                .result(courtService.getCourtById(id))
                .build();
    }
}







