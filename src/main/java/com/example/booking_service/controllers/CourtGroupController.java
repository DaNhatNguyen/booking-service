package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateCourtGroupRequest;
import com.example.booking_service.dto.request.RejectCourtGroupRequest;
import com.example.booking_service.dto.request.SoftDeleteCourtGroupRequest;
import com.example.booking_service.dto.response.CourtGroupAdminListResponse;
import com.example.booking_service.dto.response.CourtGroupDetailResponse;
import com.example.booking_service.dto.response.CourtGroupListResponse;
import com.example.booking_service.dto.response.CourtGroupResponse;
import com.example.booking_service.dto.response.CourtPriceDTO;
import com.example.booking_service.dto.response.SoftDeleteCourtGroupResponse;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.UserRepository;
// import com.example.booking_service.service.CourtAvailabilityService;
import com.example.booking_service.service.CourtGroupService;
import com.example.booking_service.service.CourtPriceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/court-groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtGroupController {

    CourtGroupService courtGroupService;
    CourtPriceService courtPriceService;
    UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<CourtGroupResponse>> getCourtGroups(@RequestParam String province,
                                                                @RequestParam String district) {
        return ApiResponse.<List<CourtGroupResponse>>builder()
                .result(courtGroupService.getCourtGroups(province, district))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CourtGroupDetailResponse> getCourtGroupById(@PathVariable Long id) {
        return ApiResponse.<CourtGroupDetailResponse>builder()
                .result(courtGroupService.getCourtGroupDetailById(id))
                .build();
    }
    
    /**
     * Get court prices with time slots for a court group
     * GET /court-groups/{courtGroupId}/prices
     */
    @GetMapping("/{courtGroupId}/prices")
    public ApiResponse<List<CourtPriceDTO>> getCourtPrices(@PathVariable Long courtGroupId) {
        List<CourtPriceDTO> prices = courtPriceService.getCourtPricesWithTimeSlots(courtGroupId);
        return ApiResponse.<List<CourtPriceDTO>>builder()
                .result(prices)
                .build();
    }
    
    /**
     * Get top rated court groups
     * Query params: limit (default: 4, max: 4)
     */
    @GetMapping("/top-rated")
    public ApiResponse<List<CourtGroupResponse>> getTopRatedCourtGroups(
            @RequestParam(defaultValue = "4") int limit) {
        
        // Limit maximum to 4
        int actualLimit = Math.min(limit, 4);
        
        return ApiResponse.<List<CourtGroupResponse>>builder()
                .result(courtGroupService.getTopRatedCourtGroups(actualLimit))
                .build();
    }
    
    /**
     * Get all court groups (Admin only)
     * Query params: status (optional), page (default: 1), limit (default: 20)
     */
    @GetMapping("/all")
    public ApiResponse<CourtGroupAdminListResponse> getAllCourtGroups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        // Check admin role
        checkAdminRole();
        
        return ApiResponse.<CourtGroupAdminListResponse>builder()
                .result(courtGroupService.getAllCourtGroups(status, page, limit))
                .build();
    }
    
    /**
     * Approve a court group (Admin only)
     */
    @PatchMapping("/{id}/approve")
    public ApiResponse<CourtGroupListResponse> approveCourtGroup(@PathVariable Long id) {
        // Check admin role
        checkAdminRole();
        
        return ApiResponse.<CourtGroupListResponse>builder()
                .message("Court group approved successfully")
                .result(courtGroupService.approveCourtGroup(id))
                .build();
    }
    
    /**
     * Reject a court group (Admin only)
     */
    @PatchMapping("/{id}/reject")
    public ApiResponse<CourtGroupListResponse> rejectCourtGroup(
            @PathVariable Long id,
            @RequestBody(required = false) RejectCourtGroupRequest request) {
        
        // Check admin role
        checkAdminRole();
        
        String reason = (request != null) ? request.getReason() : null;
        
        return ApiResponse.<CourtGroupListResponse>builder()
                .message("Court group rejected successfully")
                .result(courtGroupService.rejectCourtGroup(id, reason))
                .build();
    }
    
    /**
     * Delete a court group (Admin or Owner)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteCourtGroup(@PathVariable Long id) {
        // Check if user is admin or owner of the court group
        checkDeletePermission(id);
        
        courtGroupService.deleteCourtGroup(id);
        
        return ApiResponse.<String>builder()
                .message("Court group deleted successfully")
                .build();
    }
    
    /**
     * Soft delete a court group (Admin or Owner)
     * PATCH /court-groups/:id/soft-delete
     */
    @PatchMapping("/{id}/soft-delete")
    public ApiResponse<SoftDeleteCourtGroupResponse> softDeleteCourtGroup(
            @PathVariable Long id,
            @RequestBody SoftDeleteCourtGroupRequest request) {
        
        // Check if user is admin or owner of the court group
        checkDeletePermission(id);
        
        SoftDeleteCourtGroupResponse result = 
                courtGroupService.softDeleteCourtGroup(id, request.getIsDeleted());
        
        return ApiResponse.<SoftDeleteCourtGroupResponse>builder()
                .code(1000)
                .result(result)
                .message("Đã xóa cụm sân thành công")
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
    
    /**
     * Update court group (Owner only)
     * PUT /court-groups/{id}
     * Content-Type: multipart/form-data
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CourtGroupResponse> updateCourtGroup(
            @PathVariable Long id,
            @RequestParam("owner_id") Long ownerId,
            @RequestParam("field_name") String fieldName,
            @RequestParam("field_type") String fieldType,
            @RequestParam("address") String address,
            @RequestParam("district") String district,
            @RequestParam("province") String province,
            @RequestParam("phone") String phone,
            @RequestParam("open_time") String openTime,
            @RequestParam("close_time") String closeTime,
            @RequestParam(value = "court_number", required = false) Integer courtNumber,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "existing_images", required = false) String existingImages) {
        
        // Check if user is owner of the court group
        checkUpdatePermission(id, ownerId);
        
        CourtGroupResponse result = courtGroupService.updateCourtGroup(
                id, ownerId, fieldName, fieldType, address, district, province,
                phone, openTime, closeTime, courtNumber, description, images, existingImages);
        
        return ApiResponse.<CourtGroupResponse>builder()
                .code(1000)
                .result(result)
                .message("Cập nhật cụm sân thành công")
                .build();
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Check if the current user is an admin
     */
    private void checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        if (user.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    /**
     * Check if the current user can delete the court group (Admin or Owner)
     */
    private void checkDeletePermission(Long courtGroupId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Admin can delete any court group
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        
        // Owner can only delete their own court group
        CourtGroupDetailResponse courtGroup = courtGroupService.getCourtGroupDetailById(courtGroupId);
        if (!user.getId().equals(courtGroup.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    /**
     * Check if the user has permission to update the court group (Owner only)
     */
    private void checkUpdatePermission(Long courtGroupId, Long ownerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Admin can update any court group
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        
        // Owner can only update their own court group
        if (!user.getId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Verify the court group belongs to this owner
        CourtGroupDetailResponse courtGroup = courtGroupService.getCourtGroupDetailById(courtGroupId);
        if (!courtGroup.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}


