package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateBookingRequest;
import com.example.booking_service.dto.response.*;
import com.example.booking_service.entity.*;
import com.example.booking_service.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingService {

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    CourtRepository courtRepository;
    CourtGroupRepository courtGroupRepository;
    TimeSlotRepository timeSlotRepository;
    CourtPriceRepository courtPriceRepository;
    BookingRepository bookingRepository;
    UserRepository userRepository;

    private String formatTime(LocalTime time) {
        if (time == null) return null;
        return time.format(TIME_FORMATTER);
    }

    public BookingListResponse getBookings(
            Integer page,
            Integer limit,
            String status,
            LocalDate bookingDate,
            String search,
            Long ownerId) {
        
        try {
            // Set defaults
            int pageNumber = (page != null && page > 0) ? page - 1 : 0; // Convert to 0-based index
            int pageSize = (limit != null && limit > 0) ? limit : 10;
            
            log.info("Fetching bookings: page={}, limit={}, status={}, bookingDate={}, search={}, ownerId={}", 
                    page, limit, status, bookingDate, search, ownerId);
            
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            
            Page<Booking> bookingPage = bookingRepository.findBookingsWithFilters(
                    status,
                    bookingDate,
                    search,
                    ownerId,
                    pageable
            );
            
            // Map to response
            List<BookingDetailResponse> bookingDetails = bookingPage.getContent().stream()
                    .map(this::toBookingDetailResponse)
                    .collect(Collectors.toList());
            
            // Create pagination response
            PaginationResponse pagination = PaginationResponse.builder()
                    .total(bookingPage.getTotalElements())
                    .page(page != null ? page : 1)
                    .limit(pageSize)
                    .totalPages(bookingPage.getTotalPages())
                    .build();
            
            return BookingListResponse.builder()
                    .bookings(bookingDetails)
                    .pagination(pagination)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching bookings", e);
            throw e;
        }
    }

    public BookingDetailResponse getBookingById(Long bookingId) {
        try {
            log.info("Fetching booking by ID: {}", bookingId);
            
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            return toBookingDetailResponse(booking);
            
        } catch (Exception e) {
            log.error("Error fetching booking by ID: {}", bookingId, e);
            throw e;
        }
    }

    @Transactional
    public UpdateBookingStatusResponse updateBookingStatus(Long bookingId, String status) {
        try {
            log.info("Updating booking status: bookingId={}, newStatus={}", bookingId, status);
            
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Update status
            booking.setStatus(status);
            
            // Update timestamp
            LocalDateTime now = LocalDateTime.now();
            
            Booking updatedBooking = bookingRepository.save(booking);
            
            log.info("Booking status updated successfully: bookingId={}, status={}", bookingId, status);
            
            return UpdateBookingStatusResponse.builder()
                    .id(updatedBooking.getId())
                    .status(updatedBooking.getStatus())
                    .updatedAt(now.format(DATETIME_FORMATTER))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error updating booking status: bookingId={}", bookingId, e);
            throw e;
        }
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        try {
            log.info("Deleting/Cancelling booking: bookingId={}", bookingId);
            
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Soft delete: Set status to CANCELLED instead of hard delete
            // This preserves booking history
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            
            // Alternative: Hard delete (uncomment if needed)
            // bookingRepository.delete(booking);
            
            log.info("Booking deleted/cancelled successfully: bookingId={}", bookingId);
            
        } catch (Exception e) {
            log.error("Error deleting booking: bookingId={}", bookingId, e);
            throw e;
        }
    }

    private BookingDetailResponse toBookingDetailResponse(Booking booking) {
        // Fetch related entities
        User user = userRepository.findById(booking.getUserId().toString()).orElse(null);
        Court court = courtRepository.findById(booking.getCourtId()).orElse(null);
        CourtGroup courtGroup = null;
        
        if (court != null) {
            courtGroup = courtGroupRepository.findById(court.getCourtGroupId()).orElse(null);
        }
        
        String courtGroupAddress = "";
        if (courtGroup != null) {
            courtGroupAddress = String.format("%s, %s, %s", 
                    courtGroup.getAddress(),
                    courtGroup.getDistrict(),
                    courtGroup.getProvince());
        }
        
        return BookingDetailResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .courtId(booking.getCourtId())
                .courtName(court != null ? court.getName() : null)
                .courtGroupId(courtGroup != null ? courtGroup.getId() : null)
                .courtGroupName(courtGroup != null ? courtGroup.getName() : null)
                .courtGroupType(courtGroup != null ? courtGroup.getType() : null)
                .courtGroupAddress(courtGroupAddress)
                .bookingDate(booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : null)
                .startTime(booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) + ":00" : null)
                .endTime(booking.getEndTime() != null ? booking.getEndTime().format(TIME_FORMATTER) + ":00" : null)
                .status(booking.getStatus())
                .price(booking.getPrice())
                .createdAt(booking.getCreatedAt() != null ? booking.getCreatedAt().format(DATETIME_FORMATTER) : null)
                .address(booking.getAddress())
                .build();
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