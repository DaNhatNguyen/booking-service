package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateBookingRequest;
import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.*;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private String formatTime(LocalTime time) {
        if (time == null) return null;
        return time.format(TIME_FORMATTER);
    }

    public BookingByDateResponse getBookingDataByDate(Long courtGroupId, LocalDate date) {
        // Get all courts in the court group
        List<Court> courts = courtRepository.findByCourtGroupId(courtGroupId);
        
        if (courts.isEmpty()) {
            return BookingByDateResponse.builder()
                    .bookingCourts(Collections.emptyList())
                    .build();
        }

        // Extract court IDs
        List<Long> courtIds = courts.stream()
                .map(Court::getId)
                .collect(Collectors.toList());

        // Get all bookings for these courts on the specified date
        List<Booking> bookings = bookingRepository.findActiveBookingsByCourtIdsAndDate(courtIds, date);

        // Determine day type (WEEKDAY or WEEKEND)
        String dayType = isWeekend(date) ? "WEEKEND" : "WEEKDAY";

        // Get prices for the court group and day type
        // First try to get prices with effective_date <= date
        List<CourtPrice> courtPrices = courtPriceRepository.findActivePricesByCourtGroupAndDayType(
                courtGroupId, dayType, date);
        
        // If no prices found, get the latest prices regardless of effective_date
        if (courtPrices.isEmpty()) {
            courtPrices = courtPriceRepository.findLatestPricesByCourtGroupAndDayType(
                    courtGroupId, dayType);
        }

        // Get all time slots
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();
        Map<Long, TimeSlot> timeSlotMap = timeSlots.stream()
                .collect(Collectors.toMap(TimeSlot::getId, ts -> ts));

        // Build price info list
        List<BookingByDateResponse.PriceInfo> priceInfoList = courtPrices.stream()
                .map(cp -> {
                    TimeSlot ts = timeSlotMap.get(cp.getTimeSlotId());
                    if (ts == null) return null;
                    return BookingByDateResponse.PriceInfo.builder()
                            .timeSlotId(ts.getId())
                            .startTime(formatTime(ts.getStartTime()))
                            .endTime(formatTime(ts.getEndTime()))
                            .price(cp.getPrice())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Group bookings by court ID
        Map<Long, List<Booking>> bookingsByCourtId = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getCourtId));

        // Build court data list
        List<BookingByDateResponse.BookingCourtData> courtDataList = courts.stream()
                .map(court -> {
                    List<Booking> courtBookings = bookingsByCourtId.getOrDefault(court.getId(), Collections.emptyList());
                    
                    List<BookingByDateResponse.BookingInfo> bookingInfoList = courtBookings.stream()
                            .map(booking -> BookingByDateResponse.BookingInfo.builder()
                                    .id(booking.getId())
                                    .bookingDate(booking.getBookingDate().toString())
                                    .startTime(formatTime(booking.getStartTime()))
                                    .endTime(formatTime(booking.getEndTime()))
                                    .totalPrice(booking.getPrice())
                                    .build())
                            .collect(Collectors.toList());

                    return BookingByDateResponse.BookingCourtData.builder()
                            .id(court.getId())
                            .name(court.getName())
                            .bookings(bookingInfoList)
                            .prices(priceInfoList)
                            .build();
                })
                .collect(Collectors.toList());

        return BookingByDateResponse.builder()
                .bookingCourts(courtDataList)
                .build();
    }

    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        // Parse date and times
        LocalDate bookingDate = LocalDate.parse(request.getBookingDate());
        
        // Get earliest start time and latest end time from time slots
        LocalTime startTime = request.getTimeSlots().stream()
                .map(ts -> LocalTime.parse(ts.getStartTime(), TIME_FORMATTER))
                .min(LocalTime::compareTo)
                .orElseThrow(() -> new RuntimeException("No time slots provided"));
        
        LocalTime endTime = request.getTimeSlots().stream()
                .map(ts -> LocalTime.parse(ts.getEndTime(), TIME_FORMATTER))
                .max(LocalTime::compareTo)
                .orElseThrow(() -> new RuntimeException("No time slots provided"));
        
        // Create booking entity
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .courtId(request.getCourtId())
                .bookingDate(bookingDate)
                .startTime(startTime)
                .endTime(endTime)
                .status(request.getStatus() != null ? request.getStatus() : "PENDING")
                .price(request.getTotalPrice())
                .address(request.getFullAddress())
                .createdAt(LocalDateTime.now())
                .build();
        
        // Save booking
        Booking savedBooking = bookingRepository.save(booking);
        
        // Build response
        return CreateBookingResponse.builder()
                .bookingId(savedBooking.getId())
                .userId(savedBooking.getUserId())
                .courtId(savedBooking.getCourtId())
                .bookingDate(savedBooking.getBookingDate().toString())
                .startTime(formatTime(savedBooking.getStartTime()))
                .endTime(formatTime(savedBooking.getEndTime()))
                .status(savedBooking.getStatus())
                .totalPrice(savedBooking.getPrice())
                .address(savedBooking.getAddress())
                .message("Booking created successfully. Please proceed to payment.")
                .build();
    }

    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
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