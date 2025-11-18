package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.CourtGroupResponse;
// import com.example.booking_service.service.CourtAvailabilityService;
import com.example.booking_service.service.CourtGroupService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/court-groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtGroupController {

    CourtGroupService courtGroupService;
    // CourtAvailabilityService availabilityService;

    @GetMapping
    public ApiResponse<List<CourtGroupResponse>> getCourtGroups(@RequestParam String province,
                                                                @RequestParam String district) {
        return ApiResponse.<List<CourtGroupResponse>>builder()
                .result(courtGroupService.getCourtGroups(province, district))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourtGroupResponse> getCourtGroupById(@PathVariable Long id) {
        return ApiResponse.<CourtGroupResponse>builder()
                .result(courtGroupService.getCourtGroupById(id))
                .build();
    }
}


