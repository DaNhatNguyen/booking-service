package com.example.booking_service.service;

import com.example.booking_service.dto.response.CourtGroupResponse;
import com.example.booking_service.entity.CourtGroup;
import com.example.booking_service.entity.Favorite;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.FavoriteRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteService {

    FavoriteRepository favoriteRepository;
    CourtGroupRepository courtGroupRepository;
    CourtGroupService courtGroupService;

    /**
     * Add court group to favorites
     */
    @Transactional
    public void addFavorite(Long userId, Long courtGroupId) {
        // Check if court group exists and is not deleted
        CourtGroup courtGroup = courtGroupRepository.findById(courtGroupId)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        // Check if already in favorites
        if (favoriteRepository.existsByUserIdAndCourtGroupId(userId, courtGroupId)) {
            log.info("Court group {} already in favorites for user {}", courtGroupId, userId);
            return; // Already in favorites, no need to add again
        }
        
        // Add to favorites
        Favorite favorite = Favorite.builder()
                .userId(userId)
                .courtGroupId(courtGroupId)
                .createdAt(LocalDateTime.now())
                .build();
        
        favoriteRepository.save(favorite);
        log.info("Added court group {} to favorites for user {}", courtGroupId, userId);
    }

    /**
     * Remove court group from favorites
     */
    @Transactional
    public void removeFavorite(Long userId, Long courtGroupId) {
        if (!favoriteRepository.existsByUserIdAndCourtGroupId(userId, courtGroupId)) {
            log.info("Court group {} not in favorites for user {}", courtGroupId, userId);
            return; // Not in favorites, no need to remove
        }
        
        favoriteRepository.deleteByUserIdAndCourtGroupId(userId, courtGroupId);
        log.info("Removed court group {} from favorites for user {}", courtGroupId, userId);
    }

    /**
     * Check if court group is in favorites
     */
    public boolean isFavorite(Long userId, Long courtGroupId) {
        return favoriteRepository.existsByUserIdAndCourtGroupId(userId, courtGroupId);
    }

    /**
     * Get list of favorite court groups for user
     */
    public List<CourtGroupResponse> getFavoriteCourts(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        
        return favorites.stream()
                .map(f -> {
                    try {
                        CourtGroup courtGroup = courtGroupRepository.findById(f.getCourtGroupId())
                                .orElse(null);
                        
                        // Only return if court group exists and is not deleted
                        if (courtGroup != null && !Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
                            return courtGroupService.getCourtGroupById(courtGroup.getId());
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error getting court group {} for favorite: {}", f.getCourtGroupId(), e.getMessage());
                        return null;
                    }
                })
                .filter(courtGroup -> courtGroup != null)
                .collect(Collectors.toList());
    }
}

