package com.example.booking_service.service;

import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.*;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    CourtRepository courtRepository;
    CourtGroupRepository courtGroupRepository;
    TimeSlotRepository timeSlotRepository;
    CourtPriceRepository courtPriceRepository;
    BookingRepository bookingRepository;

    public BookingDataResponse getBookingData(Long courtGroupId, LocalDate date) {
        CourtGroup courtGroup = courtGroupRepository.findById(courtGroupId)
                .orElseThrow(() -> new RuntimeException("Court group not found"));

        List<Court> courts = courtRepository.findByCourtGroupId(courtGroupId);

        List<BookingCourtResponse> bookingCourts = courts.stream()
                .map(court -> buildBookingCourt(court, date))
                .collect(Collectors.toList());

        return BookingDataResponse.builder()
                .id(courtGroup.getId())
                .name(courtGroup.getName())
                .defaultPrice(250000.0)
                .bookingCourts(bookingCourts)
                .build();
    }

    private BookingCourtResponse buildBookingCourt(Court court, LocalDate date) {
        // Get bookings for this court on this date
        List<Booking> activeBookings = bookingRepository.findActiveBookingsByCourtAndDate(court.getId(), date);

        List<BookingSlotResponse> bookingSlots = activeBookings.stream()
                .map(booking -> {
                    TimeSlot timeSlot = timeSlotRepository.findById(booking.getTimeSlotId())
                            .orElse(null);
                    if (timeSlot == null) return null;

                    return BookingSlotResponse.builder()
                            .id(booking.getId())
                            .startTime(formatTime(timeSlot.getStartTime()))
                            .endTime(formatTime(timeSlot.getEndTime()))
                            .totalPrice(booking.getPrice())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Get prices for this court
        List<CourtPrice> courtPrices = courtPriceRepository.findActivePricesByCourtId(court.getId(), date);

        List<PricingSlotResponse> prices = courtPrices.stream()
                .map(cp -> {
                    TimeSlot timeSlot = timeSlotRepository.findById(cp.getTimeSlotId())
                            .orElse(null);
                    if (timeSlot == null) return null;

                    return PricingSlotResponse.builder()
                            .timeSlotId(cp.getTimeSlotId())
                            .startTime(formatTime(timeSlot.getStartTime()))
                            .endTime(formatTime(timeSlot.getEndTime()))
                            .price(cp.getPrice())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return BookingCourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .bookings(bookingSlots)
                .prices(prices)
                .build();
    }

    public BookingConfirmationResponse getBookingConfirmation(Long courtGroupId, Long courtId,
                                                              LocalDate date,
                                                              List<TimeSlotSelection> selectedSlots) {
        CourtGroup courtGroup = courtGroupRepository.findById(courtGroupId)
                .orElseThrow(() -> new RuntimeException("Court group not found"));

        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));

        String fullAddress = String.format("%s, %s, %s",
                courtGroup.getAddress(),
                courtGroup.getDistrict(),
                courtGroup.getProvince());

        // Merge consecutive time slots
        List<TimeSlotInfo> mergedSlots = mergeTimeSlots(selectedSlots);

        // Calculate total price
        double totalPrice = calculateTotalPrice(courtId, selectedSlots, date);

        return BookingConfirmationResponse.builder()
                .courtGroupId(courtGroup.getId())
                .courtGroupName(courtGroup.getName())
                .fullAddress(fullAddress)
                .bookingDate(date.toString())
                .courtName(court.getName())
                .timeSlots(mergedSlots)
                .totalPrice(totalPrice)
                .build();
    }

    private List<TimeSlotInfo> mergeTimeSlots(List<TimeSlotSelection> slots) {
        if (slots.isEmpty()) return Collections.emptyList();

        // Sort by start time
        List<TimeSlotSelection> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparing(TimeSlotSelection::getStartTime));

        List<TimeSlotInfo> merged = new ArrayList<>();
        TimeSlotSelection current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            TimeSlotSelection next = sorted.get(i);
            if (current.getEndTime().equals(next.getStartTime())) {
                // Consecutive, merge
                current = new TimeSlotSelection(current.getStartTime(), next.getEndTime());
            } else {
                // Not consecutive, add current and start new
                merged.add(TimeSlotInfo.builder()
                        .startTime(current.getStartTime())
                        .endTime(current.getEndTime())
                        .build());
                current = next;
            }
        }

        // Add the last one
        merged.add(TimeSlotInfo.builder()
                .startTime(current.getStartTime())
                .endTime(current.getEndTime())
                .build());

        return merged;
    }

    private double calculateTotalPrice(Long courtId, List<TimeSlotSelection> slots, LocalDate date) {
        List<CourtPrice> prices = courtPriceRepository.findActivePricesByCourtId(courtId, date);

        double total = 0.0;
        for (TimeSlotSelection slot : slots) {
            LocalTime start = LocalTime.parse(slot.getStartTime(), TIME_FORMATTER);
            LocalTime end = LocalTime.parse(slot.getEndTime(), TIME_FORMATTER);

            for (CourtPrice cp : prices) {
                TimeSlot ts = timeSlotRepository.findById(cp.getTimeSlotId()).orElse(null);
                if (ts == null) continue;

                LocalTime priceStart = ts.getStartTime();
                LocalTime priceEnd = ts.getEndTime();

                // Check if slot overlaps with price range
                if (!start.isBefore(priceStart) && start.isBefore(priceEnd) ||
                        !end.isAfter(priceStart) && end.isBefore(priceEnd) ||
                        start.isBefore(priceStart) && end.isAfter(priceEnd)) {
                    total += cp.getPrice();
                    break;
                }
            }
        }

        return total;
    }

    private String formatTime(LocalTime time) {
        if (time == null) return null;
        return time.format(TIME_FORMATTER);
    }

    // Inner class for time slot selection
    public static class TimeSlotSelection {
        private String startTime;
        private String endTime;

        public TimeSlotSelection(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
    }
}