package com.example.booking_service.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class AdminDashboardResponse {
    Overview overview;
    List<BookingTrendPoint> bookingTrend;
    List<RevenueTrendPoint> revenueTrend;
    List<DistributionItem> bookingStatusDistribution;
    List<TopCourtGroup> topCourtGroups;
    List<RecentBooking> recentBookings;
    List<ReviewHighlight> reviewHighlights;
    OwnerVerificationStats ownerVerification;
    List<UtilizationItem> utilization;

    @Value
    @Builder
    @Jacksonized
    public static class Overview {
        long totalUsers;
        long totalOwners;
        long totalBookings;
        double totalRevenue;
        long pendingOwnerRequests;
        long activeCourtGroups;
        double averageRating;
        long bookingGrowth;
        long revenueGrowth;
    }

    @Value
    @Builder
    @Jacksonized
    public static class BookingTrendPoint {
        String label;
        long bookings;
    }

    @Value
    @Builder
    @Jacksonized
    public static class RevenueTrendPoint {
        String label;
        double revenue;
    }

    @Value
    @Builder
    @Jacksonized
    public static class DistributionItem {
        String label;
        long value;
        String color;
    }

    @Value
    @Builder
    @Jacksonized
    public static class TopCourtGroup {
        Long id;
        String name;
        String address;
        String district;
        String type;
        Double rating;
        long bookings;
        double revenue;
    }

    @Value
    @Builder
    @Jacksonized
    public static class RecentBooking {
        Long id;
        String userName;
        String courtName;
        String bookingDate;
        String time;
        String status;
        double price;
    }

    @Value
    @Builder
    @Jacksonized
    public static class ReviewHighlight {
        Long id;
        String userName;
        String courtGroup;
        Integer rating;
        String comment;
        String createdAt;
    }

    @Value
    @Builder
    @Jacksonized
    public static class OwnerVerificationStats {
        long pending;
        long approved;
        long rejected;
        long banned;
    }

    @Value
    @Builder
    @Jacksonized
    public static class UtilizationItem {
        int timeSlotId;
        String label;
        long value;
        String color;
    }
}

