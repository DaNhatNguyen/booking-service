package com.example.booking_service.service;

import com.example.booking_service.dto.response.OwnerDashboardResponse;
import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.Court;
import com.example.booking_service.entity.CourtGroup;
import com.example.booking_service.entity.Review;
import com.example.booking_service.entity.TimeSlot;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.AdminDashboardPeriod;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.CourtRepository;
import com.example.booking_service.repository.ReviewRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class OwnerDashboardService {

    CourtGroupRepository courtGroupRepository;
    BookingRepository bookingRepository;
    ReviewRepository reviewRepository;
    CourtRepository courtRepository;
    TimeSlotRepository timeSlotRepository;
    UserRepository userRepository;

    static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public OwnerDashboardResponse getDashboardOverview(
            Long ownerId,
            LocalDate startDate,
            LocalDate endDate,
            AdminDashboardPeriod period) {

        // 1. Lấy danh sách court groups của owner
        List<CourtGroup> ownerCourtGroups = courtGroupRepository.findByOwnerId(ownerId);
        
        if (ownerCourtGroups.isEmpty()) {
            return buildEmptyResponse();
        }

        List<Long> courtGroupIds = ownerCourtGroups.stream()
                .map(CourtGroup::getId)
                .collect(Collectors.toList());

        // 2. Lấy danh sách courts thuộc các court groups
        List<Court> courts = courtRepository.findByCourtGroupIdIn(courtGroupIds);
        List<Long> courtIds = courts.stream()
                .map(Court::getId)
                .collect(Collectors.toList());

        if (courtIds.isEmpty()) {
            return buildEmptyResponse();
        }

        // 3. Tính toán Overview Metrics
        OwnerDashboardResponse.OverviewMetrics overview = calculateOverviewMetrics(
                ownerCourtGroups, courtIds, startDate, endDate);

        // 4. Tính toán Booking Trend (chỉ hiển thị trong khoảng period.getDays() ngày gần nhất)
        LocalDate trendStart = endDate.minusDays(period.getDays() - 1);
        if (trendStart.isBefore(startDate)) {
            trendStart = startDate;
        }
        List<OwnerDashboardResponse.SeriesPoint> bookingTrend = calculateBookingTrend(
                courtIds, trendStart, endDate, period);

        // 5. Tính toán Revenue Trend (chỉ hiển thị trong khoảng period.getDays() ngày gần nhất)
        List<OwnerDashboardResponse.SeriesPoint> revenueTrend = calculateRevenueTrend(
                courtIds, trendStart, endDate, period);

        // 6. Tính toán Booking Status Distribution
        List<OwnerDashboardResponse.StatusDistribution> statusDistribution = 
                calculateStatusDistribution(courtIds, startDate, endDate);

        // 7. Lấy Top Court Groups
        List<OwnerDashboardResponse.CourtGroupHighlight> topCourtGroups = 
                getTopCourtGroups(ownerCourtGroups, courtIds, startDate, endDate);

        // 8. Lấy Recent Bookings
        List<OwnerDashboardResponse.BookingRow> recentBookings = 
                getRecentBookings(courtIds, 10);

        // 9. Lấy Review Highlights
        List<OwnerDashboardResponse.ReviewHighlight> reviewHighlights = 
                getReviewHighlights(courtGroupIds, 5);

        // 10. Tính toán Utilization
        List<OwnerDashboardResponse.UtilizationBreakdown> utilization = 
                calculateUtilization(courtIds, startDate, endDate);

        return OwnerDashboardResponse.builder()
                .overview(overview)
                .bookingTrend(bookingTrend)
                .revenueTrend(revenueTrend)
                .bookingStatusDistribution(statusDistribution)
                .topCourtGroups(topCourtGroups)
                .recentBookings(recentBookings)
                .reviewHighlights(reviewHighlights)
                .utilization(utilization)
                .build();
    }

    private OwnerDashboardResponse.OverviewMetrics calculateOverviewMetrics(
            List<CourtGroup> courtGroups,
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate) {

        // Tổng số court groups
        int totalCourtGroups = courtGroups.size();

        // Tổng số bookings trong khoảng thời gian
        List<Booking> bookings = bookingRepository.findByCourtIdInAndBookingDateBetween(
                courtIds, startDate, endDate);

        int totalBookings = bookings.size();

        // Tổng doanh thu (chỉ tính CONFIRMED bookings)
        double totalRevenue = bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .mapToDouble(b -> b.getPrice() != null ? b.getPrice() : 0.0)
                .sum();

        // Đánh giá trung bình
        List<Review> reviews = reviewRepository.findByCourtGroupIdIn(
                courtGroups.stream().map(CourtGroup::getId).collect(Collectors.toList()));

        double averageRating = reviews.isEmpty() ? 0.0 :
                reviews.stream()
                        .mapToDouble(Review::getRating)
                        .average()
                        .orElse(0.0);

        // Phân loại bookings theo status
        long pendingBookings = bookings.stream()
                .filter(b -> "PENDING".equals(b.getStatus()))
                .count();

        long confirmedBookings = bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .count();

        long cancelledBookings = bookings.stream()
                .filter(b -> "CANCELLED".equals(b.getStatus()))
                .count();

        // Tính growth (so với kỳ trước)
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate previousStartDate = startDate.minusDays(daysDiff + 1);
        LocalDate previousEndDate = startDate.minusDays(1);

        List<Booking> previousBookings = bookingRepository.findByCourtIdInAndBookingDateBetween(
                courtIds, previousStartDate, previousEndDate);

        int previousTotalBookings = previousBookings.size();
        double previousRevenue = previousBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .mapToDouble(b -> b.getPrice() != null ? b.getPrice() : 0.0)
                .sum();

        int bookingGrowth = previousTotalBookings == 0 ? 0 :
                (int) Math.round(((double) (totalBookings - previousTotalBookings) / previousTotalBookings) * 100);
        int revenueGrowth = previousRevenue == 0 ? 0 :
                (int) Math.round(((totalRevenue - previousRevenue) / previousRevenue) * 100);
        return OwnerDashboardResponse.OverviewMetrics.builder()
                .totalCourtGroups(totalCourtGroups)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .pendingBookings((int) pendingBookings)
                .confirmedBookings((int) confirmedBookings)
                .cancelledBookings((int) cancelledBookings)
                .bookingGrowth(bookingGrowth)
                .revenueGrowth(revenueGrowth)
                .build();
    }

    private List<OwnerDashboardResponse.SeriesPoint> calculateBookingTrend(
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate,
            AdminDashboardPeriod period) {

        List<OwnerDashboardResponse.SeriesPoint> trend = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<Booking> dayBookings = bookingRepository.findByCourtIdInAndBookingDate(
                    courtIds, currentDate);

            trend.add(OwnerDashboardResponse.SeriesPoint.builder()
                    .label(currentDate.format(DATE_LABEL_FORMATTER))
                    .bookings(dayBookings.size())
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    private List<OwnerDashboardResponse.SeriesPoint> calculateRevenueTrend(
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate,
            AdminDashboardPeriod period) {

        List<OwnerDashboardResponse.SeriesPoint> trend = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<Booking> dayBookings = bookingRepository.findByCourtIdInAndBookingDate(
                    courtIds, currentDate);

            double dayRevenue = dayBookings.stream()
                    .filter(b -> "CONFIRMED".equals(b.getStatus()))
                    .mapToDouble(b -> b.getPrice() != null ? b.getPrice() : 0.0)
                    .sum();

            trend.add(OwnerDashboardResponse.SeriesPoint.builder()
                    .label(currentDate.format(DATE_LABEL_FORMATTER))
                    .revenue(dayRevenue)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trend;
    }

    private List<OwnerDashboardResponse.StatusDistribution> calculateStatusDistribution(
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate) {

        List<Booking> bookings = bookingRepository.findByCourtIdInAndBookingDateBetween(
                courtIds, startDate, endDate);

        long pending = bookings.stream().filter(b -> "PENDING".equals(b.getStatus())).count();
        long confirmed = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();
        long cancelled = bookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count();

        return Arrays.asList(
                OwnerDashboardResponse.StatusDistribution.builder()
                        .label("Đang chờ")
                        .value((int) pending)
                        .color("orange")
                        .build(),
                OwnerDashboardResponse.StatusDistribution.builder()
                        .label("Đã xác nhận")
                        .value((int) confirmed)
                        .color("teal")
                        .build(),
                OwnerDashboardResponse.StatusDistribution.builder()
                        .label("Đã hủy")
                        .value((int) cancelled)
                        .color("red")
                        .build()
        );
    }

    private List<OwnerDashboardResponse.CourtGroupHighlight> getTopCourtGroups(
            List<CourtGroup> courtGroups,
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate) {

        Map<Long, List<Court>> courtsByGroup = courtRepository.findByCourtGroupIdIn(
                courtGroups.stream().map(CourtGroup::getId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.groupingBy(Court::getCourtGroupId));

        return courtGroups.stream()
                .map(cg -> {
                    List<Long> groupCourtIds = courtsByGroup.getOrDefault(cg.getId(), Collections.emptyList())
                            .stream()
                            .map(Court::getId)
                            .collect(Collectors.toList());

                    if (groupCourtIds.isEmpty()) {
                        return OwnerDashboardResponse.CourtGroupHighlight.builder()
                                .id(cg.getId())
                                .name(cg.getName())
                                .address(cg.getAddress())
                                .district(cg.getDistrict())
                                .type(cg.getType())
                                .rating(cg.getRating() != null ? cg.getRating() : 0.0)
                                .bookings(0)
                                .revenue(0.0)
                                .build();
                    }

                    List<Booking> groupBookings = bookingRepository.findByCourtIdInAndBookingDateBetween(
                            groupCourtIds, startDate, endDate);

                    int bookings = groupBookings.size();
                    double revenue = groupBookings.stream()
                            .filter(b -> "CONFIRMED".equals(b.getStatus()))
                            .mapToDouble(b -> b.getPrice() != null ? b.getPrice() : 0.0)
                            .sum();

                    // Lấy rating từ reviews
                    List<Review> reviews = reviewRepository.findByCourtGroupId(cg.getId());
                    double rating = reviews.isEmpty() ? (cg.getRating() != null ? cg.getRating() : 0.0) :
                            reviews.stream()
                                    .mapToDouble(Review::getRating)
                                    .average()
                                    .orElse(cg.getRating() != null ? cg.getRating() : 0.0);

                    return OwnerDashboardResponse.CourtGroupHighlight.builder()
                            .id(cg.getId())
                            .name(cg.getName())
                            .address(cg.getAddress())
                            .district(cg.getDistrict())
                            .type(cg.getType())
                            .rating(Math.round(rating * 10.0) / 10.0)
                            .bookings(bookings)
                            .revenue(revenue)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<OwnerDashboardResponse.BookingRow> getRecentBookings(List<Long> courtIds, int limit) {
        return bookingRepository.findByCourtIdInOrderByCreatedAtDesc(courtIds, PageRequest.of(0, limit))
                .stream()
                .map(booking -> {
                    Court court = courtRepository.findById(booking.getCourtId()).orElse(null);
                    CourtGroup courtGroup = court != null ?
                            courtGroupRepository.findById(court.getCourtGroupId()).orElse(null) : null;
                    User user = userRepository.findById(booking.getUserId()).orElse(null);

                    String courtName = (courtGroup != null ? courtGroup.getName() + " - " : "") +
                            (court != null ? court.getName() : "");
                    String userName = user != null ? user.getFullName() : "N/A";

                    String time = (booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) : "") +
                            " - " +
                            (booking.getEndTime() != null ? booking.getEndTime().format(TIME_FORMATTER) : "");

                    return OwnerDashboardResponse.BookingRow.builder()
                            .id(booking.getId())
                            .userName(userName)
                            .courtName(courtName)
                            .bookingDate(booking.getBookingDate().toString())
                            .time(time)
                            .status(booking.getStatus())
                            .price(booking.getPrice() != null ? booking.getPrice() : 0.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<OwnerDashboardResponse.ReviewHighlight> getReviewHighlights(List<Long> courtGroupIds, int limit) {
        return reviewRepository.findByCourtGroupIdInOrderByCreatedAtDesc(courtGroupIds, PageRequest.of(0, limit))
                .stream()
                .map(review -> {
                    User user = userRepository.findById(review.getUserId()).orElse(null);
                    CourtGroup courtGroup = courtGroupRepository.findById(review.getCourtGroupId()).orElse(null);

                    return OwnerDashboardResponse.ReviewHighlight.builder()
                            .id(review.getId())
                            .userName(user != null ? user.getFullName() : "N/A")
                            .courtGroup(courtGroup != null ? courtGroup.getName() : "N/A")
                            .rating(review.getRating())
                            .comment(review.getComment())
                            .createdAt(review.getCreatedAt() != null ?
                                    review.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<OwnerDashboardResponse.UtilizationBreakdown> calculateUtilization(
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate) {

        List<TimeSlot> timeSlots = timeSlotRepository.findAll();
        List<OwnerDashboardResponse.UtilizationBreakdown> utilization = new ArrayList<>();

        LocalTime eveningStart = LocalTime.of(17, 30);
        
        for (TimeSlot timeSlot : timeSlots) {
            List<Booking> slotBookings = bookingRepository.findByCourtIdInAndBookingDateBetweenAndTimeSlotId(
                    courtIds, startDate, endDate, timeSlot.getId(), eveningStart);

            // Tính tổng số giờ có thể đặt
            long totalPossibleHours = calculateTotalPossibleHours(
                    courtIds, startDate, endDate, timeSlot);

            // Tính tổng số giờ đã đặt
            long totalBookedHours = slotBookings.stream()
                    .filter(b -> "CONFIRMED".equals(b.getStatus()))
                    .mapToLong(this::calculateBookingHours)
                    .sum();

            int utilizationPercent = totalPossibleHours == 0 ? 0 :
                    (int) Math.round((double) totalBookedHours / totalPossibleHours * 100);

            String label = String.format("%s (%s - %s)",
                    timeSlot.getId() == 1 ? "Sáng" : "Tối",
                    timeSlot.getStartTime().format(TIME_FORMATTER),
                    timeSlot.getEndTime().format(TIME_FORMATTER));

            utilization.add(OwnerDashboardResponse.UtilizationBreakdown.builder()
                    .timeSlotId(timeSlot.getId().intValue())
                    .label(label)
                    .value(utilizationPercent)
                    .color(timeSlot.getId() == 1 ? "indigo" : "violet")
                    .build());
        }

        return utilization;
    }

    private long calculateTotalPossibleHours(
            List<Long> courtIds,
            LocalDate startDate,
            LocalDate endDate,
            TimeSlot timeSlot) {

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long hoursPerDay = ChronoUnit.HOURS.between(
                timeSlot.getStartTime(),
                timeSlot.getEndTime());

        return courtIds.size() * days * hoursPerDay;
    }

    private long calculateBookingHours(Booking booking) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return 0;
        }

        return ChronoUnit.HOURS.between(
                booking.getStartTime(),
                booking.getEndTime());
    }

    private OwnerDashboardResponse buildEmptyResponse() {
        return OwnerDashboardResponse.builder()
                .overview(OwnerDashboardResponse.OverviewMetrics.builder()
                        .totalCourtGroups(0)
                        .totalBookings(0)
                        .totalRevenue(0.0)
                        .averageRating(0.0)
                        .pendingBookings(0)
                        .confirmedBookings(0)
                        .cancelledBookings(0)
                        .bookingGrowth(0)
                        .revenueGrowth(0)
                        .build())
                .bookingTrend(Collections.emptyList())
                .revenueTrend(Collections.emptyList())
                .bookingStatusDistribution(Collections.emptyList())
                .topCourtGroups(Collections.emptyList())
                .recentBookings(Collections.emptyList())
                .reviewHighlights(Collections.emptyList())
                .utilization(Collections.emptyList())
                .build();
    }
}

