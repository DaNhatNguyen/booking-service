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
import org.springframework.web.multipart.MultipartFile;

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
    FileStorageService fileStorageService;

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
        User user = userRepository.findById(booking.getUserId()).orElse(null);
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
        
        // Create booking entity with PAYING status
        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .courtId(request.getCourtId())
                .bookingDate(bookingDate)
                .startTime(startTime)
                .endTime(endTime)
                .status("PAYING")  // Set status to PAYING for payment flow
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
                .message("Đã tạo booking. Vui lòng thanh toán trong 5 phút")
                .build();
    }

    public List<UserBookingHistoryResponse> getBookingsByUserId(Long userId) {
        // Get all bookings for the user
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        // Get unique court IDs from bookings
        List<Long> courtIds = bookings.stream()
                .map(Booking::getCourtId)
                .distinct()
                .collect(Collectors.toList());

        // Get courts information
        List<Court> courts = courtRepository.findAllById(courtIds);
        Map<Long, Court> courtMap = courts.stream()
                .collect(Collectors.toMap(Court::getId, court -> court));

        // Get unique court group IDs
        List<Long> courtGroupIds = courts.stream()
                .map(Court::getCourtGroupId)
                .distinct()
                .collect(Collectors.toList());

        // Get court groups information
        List<CourtGroup> courtGroups = courtGroupRepository.findAllById(courtGroupIds);
        Map<Long, CourtGroup> courtGroupMap = courtGroups.stream()
                .collect(Collectors.toMap(CourtGroup::getId, cg -> cg));

        // Build response list
        return bookings.stream()
                .map(booking -> {
                    Court court = courtMap.get(booking.getCourtId());
                    if (court == null) return null;

                    CourtGroup courtGroup = courtGroupMap.get(court.getCourtGroupId());
                    if (courtGroup == null) return null;

                    return UserBookingHistoryResponse.builder()
                            .id(booking.getId())
                            .date(booking.getBookingDate().toString())
                            .timeSlot(UserBookingHistoryResponse.TimeSlotInfo.builder()
                                    .startTime(formatTime(booking.getStartTime()))
                                    .endTime(formatTime(booking.getEndTime()))
                                    .build())
                            .status(booking.getStatus().toLowerCase())
                            .courtName(court.getName())
                            .courtGroupName(courtGroup.getName())
                            .address(courtGroup.getAddress())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted((b1, b2) -> {
                    // Sort by date descending (newest first)
                    try {
                        LocalDate date1 = LocalDate.parse(b1.getDate());
                        LocalDate date2 = LocalDate.parse(b2.getDate());
                        return date2.compareTo(date1);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }
    public CreateBookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus("CONFIRMED");
        Booking saved = bookingRepository.save(booking);

        return CreateBookingResponse.builder()
                .bookingId(saved.getId())
                .userId(saved.getUserId())
                .courtId(saved.getCourtId())
                .bookingDate(saved.getBookingDate() != null ? saved.getBookingDate().toString() : null)
                .startTime(formatTime(saved.getStartTime()))
                .endTime(formatTime(saved.getEndTime()))
                .status(saved.getStatus())
                .totalPrice(saved.getPrice())
                .address(saved.getAddress())
                .message("Booking confirmed successfully.")
                .build();
    }

    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * API 1: Get payment information for a booking
     * Retrieves bank information of court owner for payment
     */
    public PaymentInfoResponse getPaymentInfo(Long bookingId) {
        try {
            log.info("Fetching payment info for booking: {}", bookingId);
            
            // Get booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Check if booking is in PAYING status
            if (!"PAYING".equals(booking.getStatus())) {
                throw new RuntimeException("Booking is not in PAYING status");
            }
            
            // Get court
            Court court = courtRepository.findById(booking.getCourtId())
                    .orElseThrow(() -> new RuntimeException("Court not found"));
            
            // Get court group
            CourtGroup courtGroup = courtGroupRepository.findById(court.getCourtGroupId())
                    .orElseThrow(() -> new RuntimeException("Court group not found"));
            
            // Get owner (user with OWNER role)
            User owner = userRepository.findById(courtGroup.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Owner not found"));
            
            // Build address
            String fullAddress = String.format("%s, %s, %s",
                    courtGroup.getAddress(),
                    courtGroup.getDistrict(),
                    courtGroup.getProvince());
            
            // Build time slot info
            PaymentInfoResponse.TimeSlotInfo timeSlotInfo = PaymentInfoResponse.TimeSlotInfo.builder()
                    .startTime(formatTime(booking.getStartTime()))
                    .endTime(formatTime(booking.getEndTime()))
                    .build();
            
            // Build response
            return PaymentInfoResponse.builder()
                    .bookingId(booking.getId())
                    .ownerBankName(owner.getBankName())
                    .ownerBankAccountNumber(owner.getBankAccountNumber())
                    .ownerBankAccountName(owner.getBankAccountName())
                    .ownerBankQrImage(owner.getBankQrImage())
                    .totalPrice(booking.getPrice())
                    .bookingDate(booking.getBookingDate().format(DATE_FORMATTER))
                    .timeSlots(Collections.singletonList(timeSlotInfo))
                    .courtName(court.getName())
                    .fullAddress(fullAddress)
                    .createdAt(booking.getCreatedAt().format(DATETIME_FORMATTER))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching payment info for booking: {}", bookingId, e);
            throw e;
        }
    }

    /**
     * API 2: Confirm payment with proof image
     * Upload payment proof and update status to PENDING
     */
    @Transactional
    public ConfirmPaymentResponse confirmPayment(Long bookingId, MultipartFile paymentProofFile) {
        try {
            log.info("Confirming payment for booking: {}", bookingId);
            
            // Get booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Check if booking is in PAYING status
            if (!"PAYING".equals(booking.getStatus())) {
                throw new RuntimeException("Booking is not in PAYING status");
            }
            
            // Upload payment proof file
            String fileName = fileStorageService.storeFile(paymentProofFile);
            
            // Update booking
            booking.setStatus("PENDING");
            booking.setPaymentProof(fileName);
            Booking updatedBooking = bookingRepository.save(booking);
            
            log.info("Payment confirmed successfully for booking: {}", bookingId);
            
            return ConfirmPaymentResponse.builder()
                    .bookingId(updatedBooking.getId())
                    .status(updatedBooking.getStatus())
                    .paymentProofUrl(fileName)
                    .message("Đã xác nhận thanh toán. Chúng tôi sẽ xác minh trong thời gian sớm nhất.")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error confirming payment for booking: {}", bookingId, e);
            throw new RuntimeException("Error confirming payment: " + e.getMessage());
        }
    }

    /**
     * API 3: Cancel expired booking
     * Delete booking that exceeded payment time (5 minutes)
     */
    @Transactional
    public CancelExpiredResponse cancelExpiredBooking(Long bookingId) {
        try {
            log.info("Cancelling expired booking: {}", bookingId);
            
            // Get booking
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
            
            // Check if booking is in PAYING status
            if (!"PAYING".equals(booking.getStatus())) {
                throw new RuntimeException("Booking is not in PAYING status");
            }
            
            // Check if booking is expired (created more than 5 minutes ago)
            LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(5);
            if (booking.getCreatedAt().isAfter(expiryTime)) {
                throw new RuntimeException("Booking has not expired yet");
            }
            
            // Delete booking
            bookingRepository.delete(booking);
            
            log.info("Expired booking deleted successfully: {}", bookingId);
            
            return CancelExpiredResponse.builder()
                    .bookingId(bookingId)
                    .message("Đã hủy booking do hết thời gian thanh toán")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error cancelling expired booking: {}", bookingId, e);
            throw e;
        }
    }

    /**
     * Delete expired bookings automatically
     * Called by scheduled job
     */
    @Transactional
    public int deleteExpiredBookings() {
        try {
            LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(5);
            List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore("PAYING", expiryTime);
            
            if (!expiredBookings.isEmpty()) {
                bookingRepository.deleteAll(expiredBookings);
                log.info("Deleted {} expired bookings", expiredBookings.size());
                return expiredBookings.size();
            }
            
            return 0;
        } catch (Exception e) {
            log.error("Error deleting expired bookings", e);
            return 0;
        }
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