package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateCourtRequest;
import com.example.booking_service.dto.request.UpdateCourtStatusRequest;
import com.example.booking_service.dto.response.CourtGroupResponse;
import com.example.booking_service.dto.response.CourtResponse;
import com.example.booking_service.service.CourtGroupService;
import com.example.booking_service.service.CourtService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtController {

    CourtService courtService;
    CourtGroupService courtGroupService;

    @GetMapping("/{id}")
    public ApiResponse<CourtResponse> getCourtById(@PathVariable Long id) {
        return ApiResponse.<CourtResponse>builder()
                .result(courtService.getCourtById(id))
                .build();
    }

    @GetMapping("/court-group/{courtGroupId}")
    public ApiResponse<List<CourtResponse>> getCourtsByCourtGroupId(@PathVariable Long courtGroupId) {
        return ApiResponse.<List<CourtResponse>>builder()
                .result(courtService.getCourtsByCourtGroupId(courtGroupId))
                .build();
    }

    @PostMapping
    public ApiResponse<CourtResponse> createCourt(@RequestBody CreateCourtRequest request) {
        CourtResponse response = courtService.createCourt(request);
        return ApiResponse.<CourtResponse>builder()
                .message("Đã tạo sân nhỏ thành công")
                .result(response)
                .build();
    }

    @PutMapping("/{courtId}/status")
    public ApiResponse<CourtResponse> updateCourtStatus(
            @PathVariable Long courtId,
            @RequestBody UpdateCourtStatusRequest request) {
        CourtResponse response = courtService.updateCourtStatus(courtId, request.getStatus());
        String message = "available".equalsIgnoreCase(request.getStatus()) 
                ? "Đã mở khóa sân thành công" 
                : "Đã khóa sân thành công";
        return ApiResponse.<CourtResponse>builder()
                .message(message)
                .result(response)
                .build();
    }

    @GetMapping("/search")
    public List<CourtGroupResponse> searchCourtGroups(
            @RequestParam String type,
            @RequestParam String city,
            @RequestParam String district) {
        return courtGroupService.searchCourtGroups(type, city, district);
    }
}









