package com.example.booking_service.service;

import com.example.booking_service.dto.request.ConflictCheckRequest;
import com.example.booking_service.dto.request.CreateFixedBookingRequest;
import com.example.booking_service.dto.request.PricePreviewRequest;
import com.example.booking_service.dto.response.ConflictCheckResponse;
import com.example.booking_service.dto.response.FixedBookingResponse;
import com.example.booking_service.dto.response.PricePreviewResponse;
import com.example.booking_service.entity.*;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FixedBookingService {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    FixedBookingRepository fixedBookingRepository;
    BookingRepository bookingRepository;
    CourtRepository courtRepository;
    CourtGroupRepository courtGroupRepository;
    TimeSlotRepository timeSlotRepository;
    CourtPriceRepository courtPriceRepository;

    /**
     * Calculate price preview for fixed booking
     */
    public PricePreviewResponse calculatePrice(PricePreviewRequest request) {
        // Parse dates and times
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FORMATTER);

        // Get court and court group
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));
        CourtGroup courtGroup = courtGroupRepository.findById(court.getCourtGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));

        // Get time slot ID
        Long timeSlotId = getTimeSlotId(startTime, endTime);
        if (timeSlotId == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Calculate booking dates
        List<LocalDate> bookingDates = calculateBookingDates(startDate, endDate, request.getDaysOfWeek());

        // Calculate prices
        List<PricePreviewResponse.PriceBreakdown> breakdown = new ArrayList<>();
        int weekdayCount = 0;
        int weekendCount = 0;
        double totalPrice = 0.0;
        double weekdayPricePerSession = 0.0;
        double weekendPricePerSession = 0.0;

        // Calculate duration in minutes and number of 30-minute units
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        double units = durationMinutes / 30.0; // Number of 30-minute units

        for (LocalDate date : bookingDates) {
            String dayType = getDayType(date);
            Double basePrice = getBasePrice(courtGroup.getId(), timeSlotId, dayType, date);

            if (basePrice == null || basePrice == 0.0) {
                log.warn("No price found for courtGroupId={}, timeSlotId={}, dayType={}, date={}",
                        courtGroup.getId(), timeSlotId, dayType, date);
                continue;
            }

            // Calculate final price: base price * number of 30-minute units
            Double finalPrice = basePrice * units;

            breakdown.add(PricePreviewResponse.PriceBreakdown.builder()
                    .date(date.format(DATE_FORMATTER))
                    .dayType(dayType)
                    .price(finalPrice)
                    .build());

            if ("WEEKDAY".equals(dayType)) {
                weekdayCount++;
                if (weekdayPricePerSession == 0.0) {
                    weekdayPricePerSession = basePrice; // Base price per 30 minutes
                }
            } else {
                weekendCount++;
                if (weekendPricePerSession == 0.0) {
                    weekendPricePerSession = basePrice; // Base price per 30 minutes
                }
            }

            totalPrice += finalPrice;
        }

        return PricePreviewResponse.builder()
                .totalBookings(bookingDates.size())
                .weekdayCount(weekdayCount)
                .weekendCount(weekendCount)
                .weekdayPricePerSession(weekdayPricePerSession)
                .weekendPricePerSession(weekendPricePerSession)
                .totalPrice(totalPrice)
                .breakdown(breakdown)
                .build();
    }

    /**
     * Check for conflicts with existing bookings
     */
    public ConflictCheckResponse checkConflicts(ConflictCheckRequest request) {
        // Parse dates and times
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FORMATTER);

        // Get court
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));

        // Calculate booking dates
        List<LocalDate> bookingDates = calculateBookingDates(startDate, endDate, request.getDaysOfWeek());

        // Check conflicts
        List<ConflictCheckResponse.ConflictBooking> conflicts = new ArrayList<>();

        for (LocalDate date : bookingDates) {
            List<Booking> existingBookings = bookingRepository.findActiveBookingsByCourtAndDate(
                    request.getCourtId(), date);

            for (Booking booking : existingBookings) {
                if (isTimeOverlap(startTime, endTime, booking.getStartTime(), booking.getEndTime())) {
                    conflicts.add(ConflictCheckResponse.ConflictBooking.builder()
                            .bookingDate(date.format(DATE_FORMATTER))
                            .startTime(formatTime(booking.getStartTime()))
                            .endTime(formatTime(booking.getEndTime()))
                            .courtName(court.getName())
                            .build());
                }
            }
        }

        return ConflictCheckResponse.builder()
                .conflicts(conflicts)
                .build();
    }

    /**
     * Create fixed booking and generate individual bookings
     */
    @Transactional
    public FixedBookingResponse createFixedBooking(CreateFixedBookingRequest request) {
        // Validate
        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FORMATTER);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FORMATTER);
        LocalTime startTime = LocalTime.parse(request.getStartTime(), TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(request.getEndTime(), TIME_FORMATTER);

        if (endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Check conflicts
        ConflictCheckRequest conflictRequest = ConflictCheckRequest.builder()
                .courtId(request.getCourtId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysOfWeek(request.getDaysOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        ConflictCheckResponse conflictCheck = checkConflicts(conflictRequest);
        if (!conflictCheck.getConflicts().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Get court and court group
        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_NOT_EXISTED));
        CourtGroup courtGroup = courtGroupRepository.findById(court.getCourtGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));

        // Get time slot ID
        Long timeSlotId = getTimeSlotId(startTime, endTime);
        if (timeSlotId == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Create fixed booking
        String daysOfWeekStr = request.getDaysOfWeek().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        FixedBooking fixedBooking = FixedBooking.builder()
                .userId(request.getUserId())
                .courtId(request.getCourtId())
                .startDate(startDate)
                .endDate(endDate)
                .daysOfWeek(daysOfWeekStr)
                .startTime(startTime)
                .endTime(endTime)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        FixedBooking savedFixedBooking = fixedBookingRepository.save(fixedBooking);

        // Calculate booking dates
        List<LocalDate> bookingDates = calculateBookingDates(startDate, endDate, request.getDaysOfWeek());

        // Create individual bookings
        int createdCount = 0;
        String address = String.format("%s, %s, %s",
                courtGroup.getAddress(),
                courtGroup.getDistrict(),
                courtGroup.getProvince());

        // Calculate duration in minutes and number of 30-minute units
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        double units = durationMinutes / 30.0; // Number of 30-minute units

        for (LocalDate date : bookingDates) {
            String dayType = getDayType(date);
            Double basePrice = getBasePrice(courtGroup.getId(), timeSlotId, dayType, date);

            if (basePrice == null || basePrice == 0.0) {
                log.warn("Skipping booking for date {} - no price found", date);
                continue;
            }

            // Calculate final price: base price * number of 30-minute units
            Double finalPrice = basePrice * units;

            Booking booking = Booking.builder()
                    .userId(request.getUserId())
                    .courtId(request.getCourtId())
                    .timeSlotId(timeSlotId)
                    .bookingDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status("CONFIRMED")
                    .price(finalPrice)
                    .address(address)
                    .createdAt(LocalDateTime.now())
                    .build();

            bookingRepository.save(booking);
            createdCount++;
        }

        return FixedBookingResponse.builder()
                .id(savedFixedBooking.getId())
                .userId(savedFixedBooking.getUserId())
                .courtId(savedFixedBooking.getCourtId())
                .startDate(savedFixedBooking.getStartDate().format(DATE_FORMATTER))
                .endDate(savedFixedBooking.getEndDate().format(DATE_FORMATTER))
                .daysOfWeek(savedFixedBooking.getDaysOfWeek())
                .startTime(formatTime(savedFixedBooking.getStartTime()))
                .endTime(formatTime(savedFixedBooking.getEndTime()))
                .status(savedFixedBooking.getStatus())
                .createdBookingsCount(createdCount)
                .build();
    }

    // ========== Helper Methods ==========

    /**
     * Calculate booking dates based on start date, end date, and days of week
     * Frontend format: 0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday
     * Java DayOfWeek: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
     */
    private List<LocalDate> calculateBookingDates(LocalDate startDate, LocalDate endDate, List<Integer> daysOfWeek) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Java DayOfWeek: 1=Monday, 2=Tuesday, ..., 6=Saturday, 7=Sunday
            // Frontend format: 0=Sunday, 1=Monday, 2=Tuesday, ..., 6=Saturday
            int javaDayOfWeek = current.getDayOfWeek().getValue(); // 1-7
            
            // Convert Java format to Frontend format
            // Sunday: Java 7 → Frontend 0
            // Monday-Saturday: Java 1-6 → Frontend 1-6
            int frontendDayOfWeek = (javaDayOfWeek == 7) ? 0 : javaDayOfWeek;
            
            if (daysOfWeek.contains(frontendDayOfWeek)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * Get day type: WEEKDAY or WEEKEND
     * WEEKEND: Saturday (6) or Sunday (7)
     */
    private String getDayType(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayValue = dayOfWeek.getValue(); // 1=Monday, 7=Sunday
        
        return (dayValue == 6 || dayValue == 7) ? "WEEKEND" : "WEEKDAY";
    }

    /**
     * Get time slot ID based on start time and end time
     */
    private Long getTimeSlotId(LocalTime startTime, LocalTime endTime) {
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();
        
        for (TimeSlot slot : timeSlots) {
            // Check if startTime and endTime are within the slot range
            if (startTime.compareTo(slot.getStartTime()) >= 0 && 
                endTime.compareTo(slot.getEndTime()) <= 0) {
                return slot.getId();
            }
        }
        
        // If no exact match, find the slot that contains the time range
        for (TimeSlot slot : timeSlots) {
            if (startTime.compareTo(slot.getStartTime()) >= 0 && 
                startTime.compareTo(slot.getEndTime()) < 0) {
                return slot.getId();
            }
        }
        
        return null;
    }

    /**
     * Get base price from court_prices (price per 30 minutes)
     */
    private Double getBasePrice(Long courtGroupId, Long timeSlotId, String dayType, LocalDate date) {
        // Try to get active price for the date
        List<CourtPrice> prices = courtPriceRepository.findActivePricesByCourtGroupAndDayType(
                courtGroupId, dayType, date);
        
        if (!prices.isEmpty()) {
            // Filter by time slot
            Optional<CourtPrice> matchingPrice = prices.stream()
                    .filter(p -> p.getTimeSlotId().equals(timeSlotId))
                    .findFirst();
            
            if (matchingPrice.isPresent()) {
                return matchingPrice.get().getPrice();
            }
        }
        
        // Fallback: get latest price regardless of effective date
        List<CourtPrice> latestPrices = courtPriceRepository.findLatestPricesByCourtGroupAndDayType(
                courtGroupId, dayType);
        
        return latestPrices.stream()
                .filter(p -> p.getTimeSlotId().equals(timeSlotId))
                .findFirst()
                .map(CourtPrice::getPrice)
                .orElse(0.0);
    }

    /**
     * Check if two time ranges overlap
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Format time to string
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(TIME_FORMATTER);
    }
}

