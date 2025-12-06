package com.example.booking_service.service;

import com.example.booking_service.dto.request.UpdateUserRequest;
import com.example.booking_service.dto.request.UserCreationRequest;
import com.example.booking_service.dto.request.UserUpdationRequest;
import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.OwnerStatus;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.mapper.UserMapper;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.FavoriteRepository;
import com.example.booking_service.repository.ReviewRepository;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    BookingRepository bookingRepository;
    FavoriteRepository favoriteRepository;
    ReviewRepository reviewRepository;
    CourtGroupRepository courtGroupRepository;
    FileStorageService fileStorageService;
    
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private UserMapper userMapper;

//    public UserService(UserRepository userRepository, UserMapper userMapper) {
//        this.userRepository = userRepository;
//        this.userMapper = userMapper;
//    }

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        // use mapstruct
        User user = userMapper.toUser(request);

        // user builder
//        User user = User.builder()
//                .username(request.getUsername())
//                .password(request.getPassword())
//                .firstName(request.getFirstName())
//                .lastName(request.getLastName())
//                .dob(request.getDob())
//                .build();

        // normally
//        user.setUsername(request.getUsername());
//        user.setPassword(request.getPassword());
//        user.setFirstName(request.getFirstName());
//        user.setLastName(request.getLastName());
//        user.setDob(request.getDob());

        // use Bcypt to encode
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());

//        user.setRoles(roles);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }
    
    /**
     * Get users with filtering, search and pagination (Admin only)
     */
    public UserAdminListResponse getUsersAdmin(String roleStr, String ownerStatusStr, String search, int page, int limit) {
        Role role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid role, ignore
            }
        }
        
        OwnerStatus ownerStatus = null;
        if (ownerStatusStr != null && !ownerStatusStr.isBlank()) {
            try {
                ownerStatus = OwnerStatus.valueOf(ownerStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid owner status, ignore
            }
        }
        
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<User> userPage = userRepository.findUsersWithFilters(role, ownerStatus, search, pageable);
        
        List<UserListItemResponse> users = userPage.getContent()
                .stream()
                .map(this::toListItemResponse)
                .toList();
        
        PaginationResponse pagination = PaginationResponse.builder()
                .total(userPage.getTotalElements())
                .page(page)
                .limit(limit)
                .totalPages(userPage.getTotalPages())
                .build();
        
        return UserAdminListResponse.builder()
                .result(users)
                .pagination(pagination)
                .build();
    }

    public UserResponse getMyInfo(){
        // get info user currently logged in
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String name = authentication.getName();

        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @PostAuthorize("returnObject.username == authentication.name") // chi la nguoi dung dang dang nhap moi duoc goi thong tin cua minh
    public UserResponse getUserById(String id) {
        log.info("In method getUserById");
        return userMapper.toUserResponse(userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
    
    /**
     * Get user detail by ID with statistics (Admin or owner)
     */
    public UserDetailResponse getUserDetailById(Long id) {
        // Check permission
        checkUserAccessPermission(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Get statistics
        UserStatisticsResponse statistics = getUserStatistics(id);
        
        return toDetailResponse(user, statistics);
    }

    public UserResponse updateUser(String id, UserUpdationRequest request) {
        User user = userRepository.findById(Long.parseLong(id))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);

//        user.setPassword(request.getPassword());
//        user.setFirstName(request.getFirstName());
//        user.setLastName(request.getLastName());
//        user.setDob(request.getDob());

        return userMapper.toUserResponse(userRepository.save(user));
    }
    
    /**
     * Update user information (Admin or owner)
     */
    @Transactional
    public UserDetailResponse updateUserInfo(Long id, UpdateUserRequest request) {
        // Check permission
        checkUserAccessPermission(id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Update fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        User updatedUser = userRepository.save(user);
        
        // Get statistics
        UserStatisticsResponse statistics = getUserStatistics(id);
        
        return toDetailResponse(updatedUser, statistics);
    }
    
    /**
     * Update user profile (with optional avatar upload)
     */
    @Transactional
    public UserResponse updateProfile(Long userId, String fullName, String phone, MultipartFile avatarFile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Update fullName
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        
        // Update phone
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        
        // Handle avatar upload
        if (avatarFile != null && !avatarFile.isEmpty()) {
            // Delete old avatar if exists
            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                try {
                    fileStorageService.deleteFile(user.getAvatar());
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar file: {}", user.getAvatar(), e);
                }
            }
            
            // Save new avatar
            String fileName = fileStorageService.storeFile(avatarFile);
            user.setAvatar(fileName);
        }
        
        User updatedUser = userRepository.save(user);
        
        return userMapper.toUserResponse(updatedUser);
    }

    /**
     * Approve owner registration (Admin only)
     */
    @Transactional
    public UserDetailResponse approveOwner(Long ownerId, String note) {
        User owner = getOwnerOrThrow(ownerId);
        
        if (owner.getOwnerStatus() != OwnerStatus.PENDING) {
            throw new AppException(ErrorCode.OWNER_ALREADY_APPROVED);
        }
        
        validateOwnerDocuments(owner);
        
        owner.setOwnerStatus(OwnerStatus.APPROVED);
        owner.setOwnerVerifiedAt(LocalDateTime.now());
        User updatedOwner = userRepository.save(owner);
        
        log.info("Owner {} approved by admin. Note: {}", ownerId, note);
        
        return toDetailResponse(updatedOwner, getUserStatistics(ownerId));
    }

    /**
     * Reject owner registration (Admin only)
     */
    @Transactional
    public UserDetailResponse rejectOwner(Long ownerId, String reason) {
        User owner = getOwnerOrThrow(ownerId);
        
        if (owner.getOwnerStatus() != OwnerStatus.PENDING) {
            throw new AppException(ErrorCode.OWNER_INVALID_STATUS);
        }
        
        owner.setOwnerStatus(OwnerStatus.REJECTED);
        owner.setOwnerVerifiedAt(null);
        User updatedOwner = userRepository.save(owner);
        
        log.info("Owner {} rejected by admin. Reason: {}", ownerId, reason);
        
        return toDetailResponse(updatedOwner, getUserStatistics(ownerId));
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(Long.parseLong(userId));
    }
    
    /**
     * Delete user/owner (Admin only)
     */
    @Transactional
    public String deleteUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        if (user.getRole() == Role.ADMIN) {
            throw new AppException(ErrorCode.CANNOT_DELETE_ADMIN_OWNER);
        }
        
        if (user.getRole() == Role.USER) {
            long activeBookings = bookingRepository.countActiveBookingsByUserId(userId);
            if (activeBookings > 0) {
                throw new AppException(ErrorCode.CANNOT_DELETE_USER);
            }
            
            favoriteRepository.deleteByUserId(userId);
            userRepository.delete(user);
            return "User deleted successfully";
        }
        
        if (user.getRole() == Role.OWNER) {
            long approvedGroups = courtGroupRepository.countByOwnerIdAndStatus(userId, "approved");
            long activeBookings = bookingRepository.countActiveBookingsByOwnerId(userId);
            
            if (approvedGroups > 0 || activeBookings > 0) {
                throw new AppException(ErrorCode.OWNER_CANNOT_DELETE);
            }
            
            favoriteRepository.deleteByUserId(userId);
            userRepository.delete(user);
            return "Owner deleted successfully";
        }
        
        throw new AppException(ErrorCode.CANNOT_DELETE_ADMIN_OWNER);
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Check if current user has permission to access user data
     * Admin can access all, user can only access their own
     */
    private void checkUserAccessPermission(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        // Admin can access all
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        
        // User can only access their own data
        if (!currentUser.getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
    
    /**
     * Get user statistics
     */
    private UserStatisticsResponse getUserStatistics(Long userId) {
        long totalBookings = bookingRepository.countTotalBookingsByUserId(userId);
        Double totalSpent = bookingRepository.sumTotalSpentByUserId(userId);
        long favoriteCourts = favoriteRepository.countFavoritesByUserId(userId);
        long totalReviews = reviewRepository.countByUserId(userId);
        
        return UserStatisticsResponse.builder()
                .totalBookings(totalBookings)
                .totalSpent(totalSpent != null ? totalSpent : 0.0)
                .favoriteCourts(favoriteCourts)
                .totalReviews(totalReviews)
                .build();
    }
    
    private User getOwnerOrThrow(Long ownerId) {
        return userRepository.findByIdAndRole(ownerId, Role.OWNER)
                .orElseThrow(() -> new AppException(ErrorCode.OWNER_NOT_FOUND));
    }
    
    private void validateOwnerDocuments(User owner) {
        boolean hasDocuments = StringUtils.hasText(owner.getIdCardFront())
                && StringUtils.hasText(owner.getIdCardBack())
                && StringUtils.hasText(owner.getBankName())
                && StringUtils.hasText(owner.getBankAccountNumber());
        
        if (!hasDocuments) {
            throw new AppException(ErrorCode.OWNER_MISSING_DOCUMENTS);
        }
    }
    
    /**
     * Convert User entity to UserListItemResponse
     */
    private UserListItemResponse toListItemResponse(User user) {
        return UserListItemResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .ownerStatus(user.getOwnerStatus() != null ? user.getOwnerStatus().name() : null)
                .createdAt(formatDateTime(user.getCreatedAt()))
                .updatedAt(formatDateTime(user.getUpdatedAt()))
                .idCardFront(user.getIdCardFront())
                .idCardBack(user.getIdCardBack())
                .businessLicense(null)
                .ownerVerifiedAt(formatDateTime(user.getOwnerVerifiedAt()))
                .isBlock(user.getIsBlock() != null ? user.getIsBlock() : false)
                .build();
    }
    
    /**
     * Convert User entity to UserDetailResponse with statistics
     */
    private UserDetailResponse toDetailResponse(User user, UserStatisticsResponse statistics) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .ownerStatus(user.getOwnerStatus() != null ? user.getOwnerStatus().name() : null)
                .createdAt(formatDateTime(user.getCreatedAt()))
                .updatedAt(formatDateTime(user.getUpdatedAt()))
                .idCardFront(user.getIdCardFront())
                .idCardBack(user.getIdCardBack())
                .businessLicense(null)
                .ownerVerifiedAt(formatDateTime(user.getOwnerVerifiedAt()))
                .bankQrImage(user.getBankQrImage())
                .bankName(user.getBankName())
                .bankAccountNumber(user.getBankAccountNumber())
                .bankAccountName(user.getBankAccountName())
                .statistics(statistics)
                .build();
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
    
    /**
     * Block or unblock user account
     * @param userId User ID to block/unblock
     * @param isBlock 1 = block, 0 = unblock
     * @return BlockUserResponse
     */
    @Transactional
    public com.example.booking_service.dto.response.BlockUserResponse blockUser(Long userId, Integer isBlock) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Update block status
        user.setIsBlock(isBlock == 1);
        userRepository.save(user);

        return com.example.booking_service.dto.response.BlockUserResponse.builder()
                .id(user.getId())
                .isBlock(user.getIsBlock() ? 1 : 0)
                .build();
    }
}
