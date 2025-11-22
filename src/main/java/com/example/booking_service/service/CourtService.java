package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateCourtRequest;
import com.example.booking_service.dto.response.CourtResponse;
import com.example.booking_service.entity.Court;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.CourtRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourtService {

    CourtRepository courtRepository;

    public CourtResponse getCourtById(Long id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));
        
        return toResponse(court);
    }

    public List<CourtResponse> getCourtsByCourtGroupId(Long courtGroupId) {
        return courtRepository.findByCourtGroupId(courtGroupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourtResponse createCourt(CreateCourtRequest request) {
        try {
            log.info("Creating new court: {} for court group: {}", request.getName(), request.getCourtGroupId());
            
            Court court = Court.builder()
                    .courtGroupId(request.getCourtGroupId())
                    .name(request.getName())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : 1) // Default to available
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            Court savedCourt = courtRepository.save(court);
            log.info("Court created successfully with ID: {}", savedCourt.getId());
            
            return toResponse(savedCourt);
        } catch (Exception e) {
            log.error("Error creating court", e);
            throw e;
        }
    }

    public CourtResponse updateCourtStatus(Long courtId, String status) {
        try {
            log.info("Updating status for court ID: {} to {}", courtId, status);
            
            Court court = courtRepository.findById(courtId)
                    .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));
            
            // Convert status string to is_active value
            Integer isActive = "available".equalsIgnoreCase(status) ? 1 : 0;
            
            court.setIsActive(isActive);
            court.setUpdatedAt(LocalDateTime.now());
            
            Court updatedCourt = courtRepository.save(court);
            log.info("Court status updated successfully. ID: {}, new status: {}", courtId, status);
            
            return toResponse(updatedCourt);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating court status", e);
            throw e;
        }
    }

    private CourtResponse toResponse(Court entity) {
        return CourtResponse.builder()
                .id(entity.getId())
                .courtGroupId(entity.getCourtGroupId())
                .name(entity.getName())
                .status(entity.getIsActive() != null && entity.getIsActive() == 1 ? "available" : "locked")
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}









