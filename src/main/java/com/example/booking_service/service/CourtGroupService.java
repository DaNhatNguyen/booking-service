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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    public List<CourtGroupResponse> getCourtGroups(String province, String district) {
        return courtGroupRepository.findByProvinceAndDistrict(province, district)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourtGroupResponse getCourtGroupById(Long id) {
        CourtGroup courtGroup = courtGroupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));
        
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
                .rating(0.0)
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
}


