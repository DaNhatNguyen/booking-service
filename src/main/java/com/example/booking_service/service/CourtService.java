package com.example.booking_service.service;

import com.example.booking_service.dto.response.CourtResponse;
import com.example.booking_service.entity.Court;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.CourtRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourtService {

    CourtRepository courtRepository;

    public CourtResponse getCourtById(Long id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));
        
        return toResponse(court);
    }

    private CourtResponse toResponse(Court entity) {
        return CourtResponse.builder()
                .id(entity.getId())
                .courtGroupId(entity.getCourtGroupId())
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }


}






