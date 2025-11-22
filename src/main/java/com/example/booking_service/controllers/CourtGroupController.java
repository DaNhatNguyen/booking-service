package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateCourtGroupRequest;
import com.example.booking_service.dto.response.CourtGroupResponse;
// import com.example.booking_service.service.CourtAvailabilityService;
import com.example.booking_service.service.CourtGroupService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<CourtGroupResponse>> getCourtGroupsByOwnerId(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String status) {
        return ApiResponse.<List<CourtGroupResponse>>builder()
                .result(courtGroupService.getCourtGroupsByOwnerId(ownerId, status))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourtGroupResponse> createCourtGroup(
            @RequestParam("owner_id") Long ownerId,
            @RequestParam("field_name") String fieldName,
            @RequestParam("field_type") String fieldType,
            @RequestParam("address") String address,
            @RequestParam("district") String district,
            @RequestParam("province") String province,
            @RequestParam("phone") String phone,
            @RequestParam("open_time") String openTime,
            @RequestParam("close_time") String closeTime,
            @RequestParam("court_number") Integer courtNumber,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        
        CreateCourtGroupRequest request = CreateCourtGroupRequest.builder()
                .ownerId(ownerId)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .address(address)
                .district(district)
                .province(province)
                .phone(phone)
                .openTime(openTime)
                .closeTime(closeTime)
                .courtNumber(courtNumber)
                .description(description)
                .build();

        return ApiResponse.<CourtGroupResponse>builder()
                .result(courtGroupService.createCourtGroup(request, images))
                .build();
    }
}


