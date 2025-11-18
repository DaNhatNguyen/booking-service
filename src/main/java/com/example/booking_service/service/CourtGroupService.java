package com.example.booking_service.service;

import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.*;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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


