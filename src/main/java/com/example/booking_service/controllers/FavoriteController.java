package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.CheckFavoriteResponse;
import com.example.booking_service.dto.response.CourtGroupResponse;
import com.example.booking_service.entity.User;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.service.FavoriteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteController {

    FavoriteService favoriteService;
    UserRepository userRepository;

    /**
     * Add court group to favorites
     * POST /favorites/{courtGroupId}
     */
    @PostMapping("/{courtGroupId}")
    public ApiResponse<String> addFavorite(@PathVariable Long courtGroupId) {
        Long userId = getCurrentUserId();
        favoriteService.addFavorite(userId, courtGroupId);
        
        return ApiResponse.<String>builder()
                .message("Đã thêm vào yêu thích")
                .build();
    }

    /**
     * Remove court group from favorites
     * DELETE /favorites/{courtGroupId}
     */
    @DeleteMapping("/{courtGroupId}")
    public ApiResponse<String> removeFavorite(@PathVariable Long courtGroupId) {
        Long userId = getCurrentUserId();
        favoriteService.removeFavorite(userId, courtGroupId);
        
        return ApiResponse.<String>builder()
                .message("Đã xóa khỏi yêu thích")
                .build();
    }

    /**
     * Check if court group is in favorites
     * GET /favorites/check/{courtGroupId}
     */
    @GetMapping("/check/{courtGroupId}")
    public ApiResponse<CheckFavoriteResponse> checkFavorite(@PathVariable Long courtGroupId) {
        Long userId = getCurrentUserId();
        boolean isFavorite = favoriteService.isFavorite(userId, courtGroupId);
        
        CheckFavoriteResponse response = CheckFavoriteResponse.builder()
                .result(isFavorite)
                .isFavorite(isFavorite)
                .build();
        
        return ApiResponse.<CheckFavoriteResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Get list of favorite court groups
     * GET /favorites
     */
    @GetMapping
    public ApiResponse<List<CourtGroupResponse>> getFavorites() {
        Long userId = getCurrentUserId();
        List<CourtGroupResponse> favorites = favoriteService.getFavoriteCourts(userId);
        
        return ApiResponse.<List<CourtGroupResponse>>builder()
                .result(favorites)
                .build();
    }
    
    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        return user.getId();
    }
}

