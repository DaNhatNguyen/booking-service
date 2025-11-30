package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateCourtPriceRequest;
import com.example.booking_service.dto.response.CourtPriceDTO;
import com.example.booking_service.dto.response.CourtPriceResponse;
import com.example.booking_service.entity.CourtPrice;
import com.example.booking_service.entity.TimeSlot;
import com.example.booking_service.repository.CourtPriceRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourtPriceService {

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    CourtPriceRepository courtPriceRepository;
    TimeSlotRepository timeSlotRepository;

    public List<CourtPriceResponse> getCourtPricesByCourtGroupId(Long courtGroupId) {
        try {
            log.info("Getting court prices for court group ID: {}", courtGroupId);
            List<CourtPrice> courtPrices = courtPriceRepository.findByCourtGroupId(courtGroupId);
            log.info("Found {} court prices", courtPrices.size());
            return courtPrices.stream()
                    .map(this::toResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting court prices for court group ID: {}", courtGroupId, e);
            throw e;
        }
    }

    private CourtPriceResponse toResponse(CourtPrice entity) {
        return CourtPriceResponse.builder()
                .id(entity.getId())
                .courtGroupId(entity.getCourtGroupId())
                .timeSlotId(entity.getTimeSlotId())
                .dayType(entity.getDayType())
                .price(entity.getPrice())
                .effectiveDate(formatDate(entity.getEffectiveDate()))
                .build();
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }
    
    public CourtPriceResponse createOrUpdateCourtPrice(CreateCourtPriceRequest request) {
        try {
            log.info("Creating or updating court price for court group: {}, time slot: {}, day type: {}", 
                    request.getCourtGroupId(), request.getTimeSlotId(), request.getDayType());
            
            // Parse effective date
            LocalDate effectiveDate = LocalDate.parse(request.getEffectiveDate(), DATE_FORMATTER);
            
            // Check if price already exists
            CourtPrice existingPrice = courtPriceRepository.findByCourtGroupIdAndTimeSlotIdAndDayType(
                    request.getCourtGroupId(), 
                    request.getTimeSlotId(), 
                    request.getDayType()
            );
            
            CourtPrice courtPrice;
            
            if (existingPrice != null) {
                // UPDATE existing price
                log.info("Updating existing court price with ID: {}", existingPrice.getId());
                existingPrice.setPrice(request.getPrice());
                existingPrice.setEffectiveDate(effectiveDate);
                existingPrice.setCourtId(request.getCourtId());
                courtPrice = courtPriceRepository.save(existingPrice);
            } else {
                // INSERT new price
                log.info("Creating new court price");
                courtPrice = CourtPrice.builder()
                        .courtGroupId(request.getCourtGroupId())
                        .courtId(request.getCourtId())
                        .timeSlotId(request.getTimeSlotId())
                        .dayType(request.getDayType())
                        .price(request.getPrice())
                        .effectiveDate(effectiveDate)
                        .build();
                courtPrice = courtPriceRepository.save(courtPrice);
            }
            
            log.info("Court price saved successfully with ID: {}", courtPrice.getId());
            return toResponse(courtPrice);
            
        } catch (Exception e) {
            log.error("Error creating or updating court price", e);
            throw e;
        }
    }
    
    /**
     * Get court prices with time slot information
     * Returns prices with startTime and endTime from TimeSlot
     */
    public List<CourtPriceDTO> getCourtPricesWithTimeSlots(Long courtGroupId) {
        try {
            log.info("Getting court prices with time slots for court group ID: {}", courtGroupId);
            List<CourtPrice> courtPrices = courtPriceRepository.findByCourtGroupId(courtGroupId);
            log.info("Found {} court prices", courtPrices.size());
            
            return courtPrices.stream()
                    .map(cp -> {
                        TimeSlot timeSlot = timeSlotRepository.findById(cp.getTimeSlotId())
                                .orElse(null);
                        
                        return CourtPriceDTO.builder()
                                .id(cp.getId())
                                .timeSlotId(cp.getTimeSlotId())
                                .startTime(timeSlot != null ? formatTime(timeSlot.getStartTime()) : null)
                                .endTime(timeSlot != null ? formatTime(timeSlot.getEndTime()) : null)
                                .dayType(cp.getDayType())
                                .price(cp.getPrice())
                                .build();
                    })
                    .sorted(Comparator
                            .comparing((CourtPriceDTO dto) -> dto.getDayType() != null ? dto.getDayType() : "")
                            .thenComparing(dto -> dto.getStartTime() != null ? dto.getStartTime() : ""))
                    .toList();
        } catch (Exception e) {
            log.error("Error getting court prices with time slots for court group ID: {}", courtGroupId, e);
            throw e;
        }
    }
    
    private String formatTime(java.time.LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }
}

