package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.ApproveOwnerRequest;
import com.example.booking_service.dto.request.RejectOwnerRequest;
import com.example.booking_service.dto.request.UpdateUserRequest;
import com.example.booking_service.dto.request.UserCreationRequest;
import com.example.booking_service.dto.response.UserAdminListResponse;
import com.example.booking_service.dto.response.UserDetailResponse;
import com.example.booking_service.dto.response.UserResponse;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    UserRepository userRepository;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    public ApiResponse<UserAdminListResponse> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false, name = "owner_status") String ownerStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        // Debug: Log current user authorities
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Current user: {}", authentication.getName());
        authentication.getAuthorities().forEach(auth -> 
            log.info("Authority: {}", auth.getAuthority())
        );

        // Check admin role
        checkAdminRole();

        return ApiResponse.<UserAdminListResponse>builder()
                .result(userService.getUsersAdmin(role, ownerStatus, search, page, limit))
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDetailResponse> getUserById(@PathVariable Long userId) {
        return ApiResponse.<UserDetailResponse>builder()
                .result(userService.getUserDetailById(userId))
                .build();
    }

    // get info of current user
    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }
    
    /**
     * Update user profile (JSON only, no file upload)
     * PUT /users/profile
     * Content-Type: application/json
     */
    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UserResponse> updateProfileJson(
            @RequestBody(required = false) UpdateUserRequest request) {
        
        // Get current user
        User currentUser = getCurrentUser();
        
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        
        // Validate fullName if provided
        String fullName = request.getFullName();
        if (fullName != null && fullName.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        
        UserResponse updatedUser = userService.updateProfile(
                currentUser.getId(), fullName, request.getPhone(), null);
        
        return ApiResponse.<UserResponse>builder()
                .message("Cập nhật thông tin thành công")
                .result(updatedUser)
                .build();
    }
    
    /**
     * Update user profile with optional avatar upload
     * PUT /users/profile
     * Content-Type: multipart/form-data
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfileMultipart(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        
        // Get current user
        User currentUser = getCurrentUser();
        
        // Validate fullName if provided
        if (fullName != null && fullName.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        
        UserResponse updatedUser = userService.updateProfile(
                currentUser.getId(), fullName, phone, avatarFile);
        
        return ApiResponse.<UserResponse>builder()
                .message("Cập nhật thông tin thành công")
                .result(updatedUser)
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserDetailResponse> updateUser(
            @PathVariable Long userId, 
            @RequestBody UpdateUserRequest request) {
        return ApiResponse.<UserDetailResponse>builder()
                .message("User updated successfully")
                .result(userService.updateUserInfo(userId, request))
                .build();
    }

    @PatchMapping("/{userId}/approve-owner")
    public ApiResponse<UserDetailResponse> approveOwner(
            @PathVariable Long userId,
            @RequestBody(required = false) ApproveOwnerRequest request) {
        checkAdminRole();
        var response = userService.approveOwner(userId, request != null ? request.getNote() : null);
        return ApiResponse.<UserDetailResponse>builder()
                .message("Owner approved successfully")
                .result(response)
                .build();
    }

    @PatchMapping("/{userId}/reject-owner")
    public ApiResponse<UserDetailResponse> rejectOwner(
            @PathVariable Long userId,
            @RequestBody(required = false) RejectOwnerRequest request) {
        checkAdminRole();
        var response = userService.rejectOwner(userId, request != null ? request.getReason() : null);
        return ApiResponse.<UserDetailResponse>builder()
                .message("Owner rejected successfully")
                .result(response)
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {
        // Check admin role
        checkAdminRole();
        
        String message = userService.deleteUserAdmin(userId);
        
        return ApiResponse.<String>builder()
                .message(message)
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
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
