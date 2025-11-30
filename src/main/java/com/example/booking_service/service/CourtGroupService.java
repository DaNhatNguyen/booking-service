package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateCourtGroupRequest;
import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.*;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtGroupService {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    CourtGroupRepository courtGroupRepository;
    CourtRepository courtRepository;
    BookingRepository bookingRepository;
    CourtPriceRepository courtPriceRepository;
    TimeSlotRepository timeSlotRepository;
    FileStorageService fileStorageService;
    UserRepository userRepository;

    public List<CourtGroupResponse> getCourtGroups(String province, String district) {
        return courtGroupRepository.findByProvinceAndDistrict(province, district)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourtGroupResponse getCourtGroupById(Long id) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        return toResponse(courtGroup);
    }

    public List<CourtGroupResponse> searchCourtGroups(String type, String city, String district) {
        return courtGroupRepository.findByTypeAndProvinceAndDistrict(type, city, district)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CourtGroupResponse> getCourtGroupsByOwnerId(Long ownerId, String status) {
        List<CourtGroup> courtGroups;
        
        if (status != null && !status.isBlank()) {
            courtGroups = courtGroupRepository.findByOwnerIdAndStatus(ownerId, status);
        } else {
            courtGroups = courtGroupRepository.findByOwnerId(ownerId);
        }
        
        return courtGroups.stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Get top rated court groups
     * Filters: isDeleted = false, status = 'approved' or NULL, rating > 0
     * Order by: rating DESC
     */
    public List<CourtGroupResponse> getTopRatedCourtGroups(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<CourtGroup> courtGroupPage = courtGroupRepository.findTopRatedCourtGroups(pageable);
        
        return courtGroupPage.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourtGroupResponse createCourtGroup(CreateCourtGroupRequest request, List<MultipartFile> images) {
        // Parse times
        LocalTime openTime = LocalTime.parse(request.getOpenTime(), TIME_FORMATTER);
        LocalTime closeTime = LocalTime.parse(request.getCloseTime(), TIME_FORMATTER);
        
        // Handle image uploads
        String imageUrls = "";
        if (images != null && !images.isEmpty()) {
            List<String> uploadedFileNames = fileStorageService.storeFiles(images);
            imageUrls = String.join(",", uploadedFileNames);
        }
        
        // Create court group entity
        CourtGroup courtGroup = CourtGroup.builder()
                .ownerId(request.getOwnerId())
                .name(request.getFieldName())
                .type(request.getFieldType())
                .address(request.getAddress())
                .district(request.getDistrict())
                .province(request.getProvince())
                .phoneNumber(request.getPhone())
                .description(request.getDescription())
                .image(imageUrls)
                .openTime(openTime)
                .closeTime(closeTime)
                .rating(5.0)
                .status("pending")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Save court group
        CourtGroup savedCourtGroup = courtGroupRepository.save(courtGroup);
        
        // Create courts automatically based on court number
        if (request.getCourtNumber() != null && request.getCourtNumber() > 0) {
            List<Court> courts = new ArrayList<>();
            for (int i = 1; i <= request.getCourtNumber(); i++) {
                Court court = Court.builder()
                        .courtGroupId(savedCourtGroup.getId())
                        .name("Sân " + i)
                        .isActive(1)
                        .createdAt(LocalDateTime.now())
                        .build();
                courts.add(court);
            }
            courtRepository.saveAll(courts);
        }
        
        return toResponse(savedCourtGroup);
    }

    private CourtGroupResponse toResponse(CourtGroup entity) {
        return CourtGroupResponse.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .name(entity.getName())
                .type(entity.getType())
                .address(entity.getAddress())
                .district(entity.getDistrict())
                .province(entity.getProvince())
                .phoneNumber(entity.getPhoneNumber())
                .images(parseImages(entity.getImage()))
                .openTime(formatTime(entity.getOpenTime()))
                .closeTime(formatTime(entity.getCloseTime()))
                .rating(entity.getRating())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .build();
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }

    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    // ========== ADMIN APIs ==========
    
    /**
     * Get all court groups with pagination and filtering (Admin only)
     */
    public CourtGroupAdminListResponse getAllCourtGroups(String status, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<CourtGroup> courtGroupPage = courtGroupRepository.findAllWithFilters(status, pageable);
        
        List<CourtGroupListResponse> courtGroups = courtGroupPage.getContent()
                .stream()
                .map(this::toListResponse)
                .toList();
        
        PaginationResponse pagination = PaginationResponse.builder()
                .total(courtGroupPage.getTotalElements())
                .page(page)
                .limit(limit)
                .totalPages(courtGroupPage.getTotalPages())
                .build();
        
        return CourtGroupAdminListResponse.builder()
                .result(courtGroups)
                .pagination(pagination)
                .build();
    }
    
    /**
     * Get court group detail by ID with owner info, courts, and stats
     */
    public CourtGroupDetailResponse getCourtGroupDetailById(Long id) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        return toDetailResponse(courtGroup);
    }
    
    /**
     * Approve a court group (change status from pending to approved)
     */
    @Transactional
    public CourtGroupListResponse approveCourtGroup(Long id) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        if ("approved".equals(courtGroup.getStatus())) {
            throw new AppException(ErrorCode.COURT_GROUP_ALREADY_APPROVED);
        }
        
        courtGroup.setStatus("approved");
        CourtGroup updatedCourtGroup = courtGroupRepository.save(courtGroup);
        
        return toListResponse(updatedCourtGroup);
    }
    
    /**
     * Reject a court group
     */
    @Transactional
    public CourtGroupListResponse rejectCourtGroup(Long id, String reason) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        courtGroup.setStatus("rejected");
        CourtGroup updatedCourtGroup = courtGroupRepository.save(courtGroup);
        
        return toListResponse(updatedCourtGroup);
    }
    
    /**
     * Soft delete a court group (only if no active bookings)
     */
    @Transactional
    public void deleteCourtGroup(Long id) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if already deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        // Check for active bookings
        long activeBookings = bookingRepository.countActiveBookingsByCourtGroupId(id);
        
        if (activeBookings > 0) {
            throw new AppException(ErrorCode.CANNOT_DELETE_COURT_GROUP);
        }
        
        // Soft delete: set isDeleted = true
        courtGroup.setIsDeleted(true);
        courtGroupRepository.save(courtGroup);
    }
    
    /**
     * Soft delete a court group via PATCH endpoint
     * Sets is_deleted = 1 based on request body
     */
    @Transactional
    public SoftDeleteCourtGroupResponse softDeleteCourtGroup(Long id, Integer isDeleted) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Validate is_deleted value (should be 1 for delete)
        if (isDeleted == null || isDeleted != 1) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        
        // Check if already deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        // Soft delete: set isDeleted = true (1)
        courtGroup.setIsDeleted(true);
        courtGroupRepository.save(courtGroup);
        
        return SoftDeleteCourtGroupResponse.builder()
                .id(courtGroup.getId().toString())
                .isDeleted(1)
                .build();
    }
    
    /**
     * Update court group information with image handling
     * Supports keeping existing images and uploading new ones
     */
    @Transactional
    public CourtGroupResponse updateCourtGroup(
            Long id,
            Long ownerId,
            String fieldName,
            String fieldType,
            String address,
            String district,
            String province,
            String phone,
            String openTime,
            String closeTime,
            Integer courtNumber,
            String description,
            List<MultipartFile> newImages,
            String existingImages) {
        
        // Find court group and check ownership
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
        // Check if soft deleted
        if (Boolean.TRUE.equals(courtGroup.getIsDeleted())) {
            throw new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED);
        }
        
        // Check ownership
        if (!courtGroup.getOwnerId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Parse times
        LocalTime parsedOpenTime = LocalTime.parse(openTime, TIME_FORMATTER);
        LocalTime parsedCloseTime = LocalTime.parse(closeTime, TIME_FORMATTER);
        
        // Process images
        List<String> finalImageList = new ArrayList<>();
        boolean hasExistingImages = existingImages != null && !existingImages.trim().isEmpty();
        boolean hasNewImages = newImages != null && !newImages.isEmpty();
        
        // If no existing_images and no new images, keep current images
        if (!hasExistingImages && !hasNewImages) {
            // Keep all current images
            if (courtGroup.getImage() != null && !courtGroup.getImage().isEmpty()) {
                finalImageList = parseImages(courtGroup.getImage());
            }
        } else {
            // Add existing images (if provided)
            if (hasExistingImages) {
                List<String> existingImageList = parseImages(existingImages);
                // Verify existing images exist in current court group
                List<String> currentImages = parseImages(courtGroup.getImage());
                for (String existingImg : existingImageList) {
                    if (currentImages.contains(existingImg.trim())) {
                        finalImageList.add(existingImg.trim());
                    }
                }
            }
            
            // Upload and add new images
            if (hasNewImages) {
                List<String> uploadedFileNames = fileStorageService.storeFiles(newImages);
                finalImageList.addAll(uploadedFileNames);
            }
            
            // Delete old images that are no longer used
            if (courtGroup.getImage() != null && !courtGroup.getImage().isEmpty()) {
                List<String> currentImageList = parseImages(courtGroup.getImage());
                for (String oldImage : currentImageList) {
                    if (!finalImageList.contains(oldImage)) {
                        // Delete file from storage
                        try {
                            fileStorageService.deleteFile(oldImage);
                        } catch (Exception e) {
                            log.warn("Failed to delete old image file: {}", oldImage, e);
                        }
                    }
                }
            }
        }
        
        // Build final image string
        String finalImageString = finalImageList.isEmpty() 
                ? "" 
                : String.join(",", finalImageList);
        
        // Update court group fields
        courtGroup.setName(fieldName);
        courtGroup.setType(fieldType);
        courtGroup.setAddress(address);
        courtGroup.setDistrict(district);
        courtGroup.setProvince(province);
        courtGroup.setPhoneNumber(phone);
        courtGroup.setOpenTime(parsedOpenTime);
        courtGroup.setCloseTime(parsedCloseTime);
        courtGroup.setDescription(description);
        courtGroup.setImage(finalImageString);
        // Note: rating, status, created_at, is_deleted are NOT updated
        
        // Update court number if changed
        if (courtNumber != null && courtNumber > 0) {
            List<Court> existingCourts = courtRepository.findByCourtGroupId(id);
            int currentCourtCount = existingCourts.size();
            
            if (courtNumber > currentCourtCount) {
                // Add new courts
                List<Court> newCourts = new ArrayList<>();
                for (int i = currentCourtCount + 1; i <= courtNumber; i++) {
                    Court court = Court.builder()
                            .courtGroupId(id)
                            .name("Sân " + i)
                            .isActive(1)
                            .createdAt(LocalDateTime.now())
                            .build();
                    newCourts.add(court);
                }
                courtRepository.saveAll(newCourts);
            } else if (courtNumber < currentCourtCount) {
                // Remove excess courts (soft delete by setting isActive = 0)
                for (int i = courtNumber; i < currentCourtCount; i++) {
                    Court court = existingCourts.get(i);
                    court.setIsActive(0);
                    courtRepository.save(court);
                }
            }
        }
        
        CourtGroup updatedCourtGroup = courtGroupRepository.save(courtGroup);
        
        return toResponse(updatedCourtGroup);
    }
    
    // ========== Helper Methods ==========
    
    private CourtGroupListResponse toListResponse(CourtGroup entity) {
        OwnerResponse owner = null;
        if (entity.getOwnerId() != null) {
            owner = getUserById(entity.getOwnerId());
        }
        
        return CourtGroupListResponse.builder()
                .id(entity.getId())
                .stringId(entity.getId() != null ? entity.getId().toString() : null)
                .name(entity.getName())
                .type(entity.getType())
                .address(entity.getAddress())
                .district(entity.getDistrict())
                .province(entity.getProvince())
                .phone(entity.getPhoneNumber())
                .description(entity.getDescription())
                .image(entity.getImage())
                .rating(entity.getRating())
                .openTime(formatTime(entity.getOpenTime()))
                .closeTime(formatTime(entity.getCloseTime()))
                .status(entity.getStatus())
                .createdAt(formatDateTime(entity.getCreatedAt()))
                .ownerId(entity.getOwnerId())
                .owner(owner)
                .build();
    }
    
    private CourtGroupDetailResponse toDetailResponse(CourtGroup entity) {
        OwnerResponse owner = null;
        if (entity.getOwnerId() != null) {
            owner = getUserById(entity.getOwnerId());
        }
        
        // Get courts
        List<Court> courts = courtRepository.findByCourtGroupId(entity.getId());
        List<CourtSummaryResponse> courtSummaries = courts.stream()
                .map(this::toCourtSummary)
                .toList();
        
        // Get stats
        long totalBookings = bookingRepository.countTotalBookingsByCourtGroupId(entity.getId());
        Double totalRevenue = bookingRepository.sumRevenueByCourtGroupId(entity.getId());
        
        return CourtGroupDetailResponse.builder()
                .id(entity.getId())
                .stringId(entity.getId() != null ? entity.getId().toString() : null)
                .name(entity.getName())
                .type(entity.getType())
                .address(entity.getAddress())
                .district(entity.getDistrict())
                .province(entity.getProvince())
                .phone(entity.getPhoneNumber())
                .description(entity.getDescription())
                .image(entity.getImage())
                .rating(entity.getRating())
                .openTime(formatTime(entity.getOpenTime()))
                .closeTime(formatTime(entity.getCloseTime()))
                .status(entity.getStatus())
                .createdAt(formatDateTime(entity.getCreatedAt()))
                .ownerId(entity.getOwnerId())
                .owner(owner)
                .courts(courtSummaries)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .build();
    }
    
    private CourtSummaryResponse toCourtSummary(Court court) {
        return CourtSummaryResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .status(court.getIsActive() == 1 ? "available" : "unavailable")
                .isActive(court.getIsActive())
                .build();
    }
    
    private OwnerResponse getUserById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        return OwnerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return dateTime.format(formatter);
    }
}


