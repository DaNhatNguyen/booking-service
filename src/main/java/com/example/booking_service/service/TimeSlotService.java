package com.example.booking_service.service;

import com.example.booking_service.dto.response.TimeSlotResponse;
import com.example.booking_service.entity.TimeSlot;
import com.example.booking_service.repository.TimeSlotRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TimeSlotService {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    TimeSlotRepository timeSlotRepository;

    public List<TimeSlotResponse> getAllTimeSlots() {
        return timeSlotRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TimeSlotResponse toResponse(TimeSlot entity) {
        return TimeSlotResponse.builder()
                .id(entity.getId())
                .startTime(formatTime(entity.getStartTime()))
                .endTime(formatTime(entity.getEndTime()))
                .build();
    }

    private String formatTime(java.time.LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }
}







