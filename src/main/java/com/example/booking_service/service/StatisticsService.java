package com.example.booking_service.service;

import com.example.booking_service.dto.response.StatisticsResponse;
import com.example.booking_service.enums.Role;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.ReviewRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class StatisticsService {

    UserRepository userRepository;
    BookingRepository bookingRepository;
    CourtGroupRepository courtGroupRepository;
    ReviewRepository reviewRepository;
    TimeSlotRepository timeSlotRepository;

    static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public StatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate, String period) {
        // Validate period
        if (!Arrays.asList("day", "week", "month").contains(period)) {
            period = "day";
        }

        return StatisticsResponse.builder()
                .overview(buildOverview(startDate, endDate))
                .bookingStats(buildBookingStats(startDate, endDate))
                .revenueChart(buildRevenueChart(startDate, endDate, period))
                .topCourtGroups(buildTopCourtGroups(startDate, endDate))
                .userStats(buildUserStats())
                .courtGroupStats(buildCourtGroupStats())
                .build();
    }

    private StatisticsResponse.Overview buildOverview(LocalDate startDate, LocalDate endDate) {
        // Tổng số người dùng (role = USER)
        long totalUsers = userRepository.countByRole(Role.USER);

        // Tổng số chủ sân (role = OWNER)
        long totalOwners = userRepository.countByRole(Role.OWNER);

        // Tổng số booking trong khoảng thời gian
        long totalBookings = bookingRepository.countByBookingDateBetween(startDate, endDate);

        // Tổng doanh thu (tổng price của booking CONFIRMED)
        Double totalRevenue = bookingRepository.sumRevenueByBookingDateBetweenAndStatus(
                startDate, endDate, "CONFIRMED");
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        // Số cụm sân đang hoạt động (status = 'approved' và is_deleted = 0)
        long activeCourtGroups = courtGroupRepository.countByStatus("approved");

        // Đánh giá trung bình
        Double averageRating = reviewRepository.findAverageRating();
        if (averageRating == null) {
            averageRating = 0.0;
        }

        return StatisticsResponse.Overview.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .activeCourtGroups(activeCourtGroups)
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .build();
    }

    private StatisticsResponse.BookingStats buildBookingStats(LocalDate startDate, LocalDate endDate) {
        // Booking stats by status
        List<Object[]> statusStats = bookingRepository.countBookingsByStatus(startDate, endDate);
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : statusStats) {
            String status = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            byStatus.put(status, count);
        }

        // Booking stats by time slot
        List<StatisticsResponse.TimeSlotStat> byTimeSlot = buildTimeSlotStats(startDate, endDate);

        return StatisticsResponse.BookingStats.builder()
                .byStatus(byStatus)
                .byTimeSlot(byTimeSlot)
                .build();
    }

    private List<StatisticsResponse.TimeSlotStat> buildTimeSlotStats(LocalDate startDate, LocalDate endDate) {
        LocalTime eveningStart = LocalTime.of(17, 30);
        List<Object[]> timeSlotStats = bookingRepository.countBookingsByTimeSlot(startDate, endDate, eveningStart);
        Map<Long, StatisticsResponse.TimeSlotStat> timeSlotMap = new HashMap<>();

        // Initialize với tất cả time slots
        timeSlotRepository.findAll().forEach(ts -> {
            String timeSlotName = String.format("%s - %s",
                    ts.getStartTime().format(TIME_FORMATTER),
                    ts.getEndTime().format(TIME_FORMATTER));
            timeSlotMap.put(ts.getId(), StatisticsResponse.TimeSlotStat.builder()
                    .timeSlotId(ts.getId())
                    .timeSlotName(timeSlotName)
                    .count(0L)
                    .revenue(0.0)
                    .build());
        });

        // Update với dữ liệu thực tế
        for (Object[] row : timeSlotStats) {
            Long timeSlotId = row[0] != null ? ((Number) row[0]).longValue() : null;
            if (timeSlotId != null && timeSlotMap.containsKey(timeSlotId)) {
                Long count = ((Number) row[1]).longValue();
                Double revenue = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                
                timeSlotMap.put(timeSlotId, StatisticsResponse.TimeSlotStat.builder()
                        .timeSlotId(timeSlotId)
                        .timeSlotName(timeSlotMap.get(timeSlotId).getTimeSlotName())
                        .count(count)
                        .revenue(revenue)
                        .build());
            }
        }

        return new ArrayList<>(timeSlotMap.values());
    }

    private List<StatisticsResponse.RevenueChartPoint> buildRevenueChart(
            LocalDate startDate, LocalDate endDate, String period) {
        
        List<Object[]> chartData;
        if ("week".equals(period)) {
            chartData = bookingRepository.sumRevenueByWeek(startDate, endDate);
        } else if ("month".equals(period)) {
            chartData = bookingRepository.sumRevenueByMonth(startDate, endDate);
        } else {
            chartData = bookingRepository.sumRevenueByDay(startDate, endDate);
        }

        return chartData.stream()
                .map(row -> {
                    String date = (String) row[0];
                    Double revenue = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    Long bookings = ((Number) row[2]).longValue();
                    
                    return StatisticsResponse.RevenueChartPoint.builder()
                            .date(date)
                            .revenue(revenue)
                            .bookings(bookings)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StatisticsResponse.TopCourtGroup> buildTopCourtGroups(LocalDate startDate, LocalDate endDate) {
        List<Object[]> topGroups = bookingRepository.findTopCourtGroupsByRevenue(startDate, endDate, 10);
        
        return topGroups.stream()
                .map(row -> StatisticsResponse.TopCourtGroup.builder()
                        .id(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .address((String) row[2])
                        .type((String) row[3])
                        .bookingCount(((Number) row[4]).longValue())
                        .revenue(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                        .rating(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private StatisticsResponse.UserStats buildUserStats() {
        // User stats by role
        List<Object[]> roleStats = userRepository.countUsersByRole();
        Map<String, Long> byRole = new HashMap<>();
        for (Object[] row : roleStats) {
            String role = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            byRole.put(role, count);
        }

        // User stats by owner status
        List<Object[]> ownerStatusStats = userRepository.countOwnersByStatus();
        Map<String, Long> byOwnerStatus = new HashMap<>();
        for (Object[] row : ownerStatusStats) {
            String status = row[0] != null ? (String) row[0] : "NULL";
            Long count = ((Number) row[1]).longValue();
            byOwnerStatus.put(status, count);
        }

        return StatisticsResponse.UserStats.builder()
                .byRole(byRole)
                .byOwnerStatus(byOwnerStatus)
                .build();
    }

    private StatisticsResponse.CourtGroupStats buildCourtGroupStats() {
        // Court group stats by status
        List<Object[]> statusStats = courtGroupRepository.countCourtGroupsByStatus();
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : statusStats) {
            String status = row[0] != null ? (String) row[0] : "NULL";
            Long count = ((Number) row[1]).longValue();
            byStatus.put(status, count);
        }

        // Court group stats by type
        List<Object[]> typeStats = courtGroupRepository.countCourtGroupsByType();
        List<StatisticsResponse.TypeStat> byType = typeStats.stream()
                .map(row -> StatisticsResponse.TypeStat.builder()
                        .type((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return StatisticsResponse.CourtGroupStats.builder()
                .byStatus(byStatus)
                .byType(byType)
                .build();
    }
}

