package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.TimeSlotResponse;
import com.example.booking_service.service.TimeSlotService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/time-slots")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TimeSlotController {

    TimeSlotService timeSlotService;

    @GetMapping
    public ApiResponse<List<TimeSlotResponse>> getAllTimeSlots() {
        return ApiResponse.<List<TimeSlotResponse>>builder()
                .result(timeSlotService.getAllTimeSlots())
                .build();
    }
}






