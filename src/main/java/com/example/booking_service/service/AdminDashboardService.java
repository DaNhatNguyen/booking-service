package com.example.booking_service.service;

import com.example.booking_service.dto.response.AdminDashboardResponse;
import com.example.booking_service.entity.TimeSlot;
import com.example.booking_service.enums.AdminDashboardPeriod;
import com.example.booking_service.enums.OwnerStatus;
import com.example.booking_service.enums.Role;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.ReviewRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.repository.projection.RecentBookingProjection;
import com.example.booking_service.repository.projection.TopCourtGroupProjection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardService {

    BookingRepository bookingRepository;
    UserRepository userRepository;
    CourtGroupRepository courtGroupRepository;
    ReviewRepository reviewRepository;
    TimeSlotRepository timeSlotRepository;

    static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, StatusMeta> STATUS_META = Map.of(
            "PENDING", new StatusMeta("Đang chờ", "orange"),
            "CONFIRMED", new StatusMeta("Đã xác nhận", "teal"),
            "CANCELLED", new StatusMeta("Đã hủy", "red"),
            "COMPLETED", new StatusMeta("Đã hoàn thành", "green")
    );

    private static final Map<Integer, SlotMeta> DEFAULT_SLOT_META = Map.of(
            1, new SlotMeta(1, "Sáng (05:00 - 17:30)", "indigo"),
            2, new SlotMeta(2, "Tối (17:30 - 23:00)", "violet")
    );
    
    private static final LocalTime EVENING_START = LocalTime.of(17, 30);

    public AdminDashboardResponse getDashboardOverview(LocalDate startDate,
                                                       LocalDate endDate,
                                                       AdminDashboardPeriod period) {
        LocalDate normalizedStart = startDate;
        LocalDate normalizedEnd = endDate;

        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.countByBookingDateBetween(normalizedStart, normalizedEnd);
        double totalRevenue = Optional.ofNullable(
                bookingRepository.sumRevenueByBookingDateBetween(normalizedStart, normalizedEnd)).orElse(0.0);

        long totalOwners = userRepository.countByRoleAndOwnerStatus(Role.OWNER, OwnerStatus.APPROVED);
        long pendingOwners = userRepository.countByRoleAndOwnerStatus(Role.OWNER, OwnerStatus.PENDING);
        long activeCourtGroups = courtGroupRepository.countByStatus("approved");
        double averageRating = Optional.ofNullable(reviewRepository.findAverageRating()).orElse(0.0);

        long rangeDays = ChronoUnit.DAYS.between(normalizedStart, normalizedEnd) + 1;
        if (rangeDays < 1) {
            rangeDays = 1;
        }
        LocalDate previousEnd = normalizedStart.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(rangeDays - 1);

        long previousBookings = previousStart.isAfter(previousEnd)
                ? 0
                : bookingRepository.countByBookingDateBetween(previousStart, previousEnd);
        double previousRevenue = previousStart.isAfter(previousEnd)
                ? 0
                : Optional.ofNullable(bookingRepository.sumRevenueByBookingDateBetween(previousStart, previousEnd)).orElse(0.0);

        long bookingGrowth = calculateGrowthPercentage(totalBookings, previousBookings);
        long revenueGrowth = calculateGrowthPercentage(totalRevenue, previousRevenue);

        LocalDate trendStart = normalizedEnd.minusDays(period.getDays() - 1);
        if (trendStart.isBefore(normalizedStart)) {
            trendStart = normalizedStart;
        }

        List<AdminDashboardResponse.BookingTrendPoint> bookingTrend = buildBookingTrend(trendStart, normalizedEnd);
        List<AdminDashboardResponse.RevenueTrendPoint> revenueTrend = buildRevenueTrend(trendStart, normalizedEnd);
        List<AdminDashboardResponse.DistributionItem> statusDistribution =
                buildStatusDistribution(normalizedStart, normalizedEnd);
        List<AdminDashboardResponse.TopCourtGroup> topCourtGroups =
                buildTopCourtGroups(normalizedStart, normalizedEnd);
        List<AdminDashboardResponse.RecentBooking> recentBookings = buildRecentBookings();
        List<AdminDashboardResponse.ReviewHighlight> reviewHighlights = buildReviewHighlights();
        List<AdminDashboardResponse.UtilizationItem> utilization = buildUtilization(normalizedStart, normalizedEnd);

        AdminDashboardResponse.OwnerVerificationStats ownerVerificationStats = AdminDashboardResponse.OwnerVerificationStats.builder()
                .pending(pendingOwners)
                .approved(userRepository.countByRoleAndOwnerStatus(Role.OWNER, OwnerStatus.APPROVED))
                .rejected(userRepository.countByRoleAndOwnerStatus(Role.OWNER, OwnerStatus.REJECTED))
                .banned(userRepository.countByRoleAndOwnerStatus(Role.OWNER, OwnerStatus.BANNED))
                .build();

        AdminDashboardResponse.Overview overview = AdminDashboardResponse.Overview.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .pendingOwnerRequests(pendingOwners)
                .activeCourtGroups(activeCourtGroups)
                .averageRating(roundDouble(averageRating, 2))
                .bookingGrowth(bookingGrowth)
                .revenueGrowth(revenueGrowth)
                .build();

        return AdminDashboardResponse.builder()
                .overview(overview)
                .bookingTrend(bookingTrend)
                .revenueTrend(revenueTrend)
                .bookingStatusDistribution(statusDistribution)
                .topCourtGroups(topCourtGroups)
                .recentBookings(recentBookings)
                .reviewHighlights(reviewHighlights)
                .ownerVerification(ownerVerificationStats)
                .utilization(utilization)
                .build();
    }

    private List<AdminDashboardResponse.BookingTrendPoint> buildBookingTrend(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> counts = new HashMap<>();
        bookingRepository.countBookingsPerDay(startDate, endDate)
                .forEach(row -> counts.put((LocalDate) row[0], ((Number) row[1]).longValue()));

        List<AdminDashboardResponse.BookingTrendPoint> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long value = counts.getOrDefault(date, 0L);
            result.add(AdminDashboardResponse.BookingTrendPoint.builder()
                    .label(date.format(DATE_LABEL_FORMATTER))
                    .bookings(value)
                    .build());
        }
        return result;
    }

    private List<AdminDashboardResponse.RevenueTrendPoint> buildRevenueTrend(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Double> totals = new HashMap<>();
        bookingRepository.sumRevenuePerDay(startDate, endDate)
                .forEach(row -> totals.put((LocalDate) row[0], ((Number) row[1]).doubleValue()));

        List<AdminDashboardResponse.RevenueTrendPoint> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            double value = totals.getOrDefault(date, 0.0);
            result.add(AdminDashboardResponse.RevenueTrendPoint.builder()
                    .label(date.format(DATE_LABEL_FORMATTER))
                    .revenue(roundDouble(value, 2))
                    .build());
        }
        return result;
    }

    private List<AdminDashboardResponse.DistributionItem> buildStatusDistribution(LocalDate startDate, LocalDate endDate) {
        Map<String, Long> distribution = new HashMap<>();
        bookingRepository.countBookingsByStatus(startDate, endDate)
                .forEach(row -> distribution.put((String) row[0], ((Number) row[1]).longValue()));

        List<AdminDashboardResponse.DistributionItem> items = new ArrayList<>();
        distribution.forEach((status, value) -> {
            StatusMeta meta = STATUS_META.getOrDefault(status, new StatusMeta(status, "gray"));
            items.add(AdminDashboardResponse.DistributionItem.builder()
                    .label(meta.label())
                    .value(value)
                    .color(meta.color())
                    .build());
        });
        return items;
    }

    private List<AdminDashboardResponse.TopCourtGroup> buildTopCourtGroups(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findTopCourtGroups(startDate, endDate, PageRequest.of(0, 3))
                .getContent()
                .stream()
                .map(this::mapTopCourtGroup)
                .toList();
    }

    private AdminDashboardResponse.TopCourtGroup mapTopCourtGroup(TopCourtGroupProjection projection) {
        return AdminDashboardResponse.TopCourtGroup.builder()
                .id(projection.getCourtGroupId())
                .name(projection.getCourtGroupName())
                .address(projection.getAddress())
                .district(projection.getDistrict())
                .type(projection.getType())
                .rating(projection.getRating())
                .bookings(projection.getBookings() != null ? projection.getBookings() : 0)
                .revenue(roundDouble(Optional.ofNullable(projection.getRevenue()).orElse(0.0), 2))
                .build();
    }

    private List<AdminDashboardResponse.RecentBooking> buildRecentBookings() {
        return bookingRepository.findRecentBookings(PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(this::mapRecentBooking)
                .toList();
    }

    private AdminDashboardResponse.RecentBooking mapRecentBooking(RecentBookingProjection projection) {
        String courtName = projection.getCourtGroupName() + " - " + projection.getCourtName();
        String timeRange = formatTimeRange(projection.getStartTime(), projection.getEndTime());
        return AdminDashboardResponse.RecentBooking.builder()
                .id(projection.getBookingId())
                .userName(projection.getUserName())
                .courtName(courtName)
                .bookingDate(formatDate(projection.getBookingDate()))
                .time(timeRange)
                .status(projection.getStatus())
                .price(roundDouble(Optional.ofNullable(projection.getPrice()).orElse(0.0), 2))
                .build();
    }

    private List<AdminDashboardResponse.ReviewHighlight> buildReviewHighlights() {
        return reviewRepository.findReviewHighlights(PageRequest.of(0, 4))
                .getContent()
                .stream()
                .map(projection -> AdminDashboardResponse.ReviewHighlight.builder()
                        .id(projection.getReviewId())
                        .userName(projection.getUserName())
                        .courtGroup(projection.getCourtGroupName())
                        .rating(projection.getRating())
                        .comment(projection.getComment())
                        .createdAt(formatDateTime(projection.getCreatedAt()))
                        .build())
                .toList();
    }

    private List<AdminDashboardResponse.UtilizationItem> buildUtilization(LocalDate startDate, LocalDate endDate) {
        Map<Integer, SlotMeta> slotMeta = resolveSlotMeta();
        Map<Integer, Long> counts = new HashMap<>();
        bookingRepository.countUtilizationByTimeSlot(startDate, endDate, EVENING_START)
                .forEach(row -> {
                    Integer slotId = row[0] != null ? ((Number) row[0]).intValue() : null;
                    if (slotId != null) {
                        counts.put(slotId, ((Number) row[1]).longValue());
                    }
                });

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long divisor = total == 0 ? 1 : total;

        counts.keySet().forEach(slotId -> slotMeta.computeIfAbsent(slotId,
                id -> new SlotMeta(id, "Slot " + id, "gray")));

        List<AdminDashboardResponse.UtilizationItem> items = new ArrayList<>();
        slotMeta.forEach((slotId, meta) -> {
            long value = counts.getOrDefault(slotId, 0L);
            long percentage = Math.round((value * 100.0) / divisor);
            items.add(AdminDashboardResponse.UtilizationItem.builder()
                    .timeSlotId(meta.timeSlotId())
                    .label(meta.label())
                    .value(percentage)
                    .color(meta.color())
                    .build());
        });
        return items;
    }

    private long calculateGrowthPercentage(double current, double previous) {
        if (previous <= 0) {
            return current > 0 ? 100 : 0;
        }
        return Math.round(((current - previous) / previous) * 100);
    }

    private double roundDouble(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return "N/A";
        }
        return start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }

    private Map<Integer, SlotMeta> resolveSlotMeta() {
        List<TimeSlot> slots = timeSlotRepository.findAll();
        if (slots.isEmpty()) {
            return new LinkedHashMap<>(DEFAULT_SLOT_META);
        }

        Map<Integer, SlotMeta> map = new LinkedHashMap<>();
        slots.stream()
                .sorted(Comparator.comparing(TimeSlot::getStartTime, Comparator.nullsLast(LocalTime::compareTo)))
                .forEach(slot -> {
                    int id = slot.getId().intValue();
                    SlotMeta defaultMeta = DEFAULT_SLOT_META.get(id);
                    String color = defaultMeta != null ? defaultMeta.color() : "gray";
                    String label = formatSlotLabel(slot, defaultMeta);
                    map.put(id, new SlotMeta(id, label, color));
                });

        return map.isEmpty() ? new LinkedHashMap<>(DEFAULT_SLOT_META) : map;
    }

    private String formatSlotLabel(TimeSlot slot, SlotMeta defaultMeta) {
        LocalTime start = slot.getStartTime();
        LocalTime end = slot.getEndTime();
        if (start == null || end == null) {
            return defaultMeta != null ? defaultMeta.label() : "Slot " + slot.getId();
        }

        String range = start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);
        if (defaultMeta != null) {
            String prefix = defaultMeta.label().split("\\(")[0].trim();
            return prefix + " (" + range + ")";
        }
        String prefix = start.isBefore(EVENING_START) ? "Sáng" : "Tối";
        return prefix + " (" + range + ")";
    }

    private record StatusMeta(String label, String color) {}
    private record SlotMeta(int timeSlotId, String label, String color) {}
}

