package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
public class StatisticsResponse {
    Overview overview;
    BookingStats bookingStats;
    List<RevenueChartPoint> revenueChart;
    
    @JsonProperty("topCourtGroups")
    List<TopCourtGroup> topCourtGroups;
    
    @JsonProperty("userStats")
    UserStats userStats;
    
    @JsonProperty("courtGroupStats")
    CourtGroupStats courtGroupStats;

    @Value
    @Builder
    @Jacksonized
    public static class Overview {
        @JsonProperty("totalUsers")
        Long totalUsers;
        
        @JsonProperty("totalOwners")
        Long totalOwners;
        
        @JsonProperty("totalBookings")
        Long totalBookings;
        
        @JsonProperty("totalRevenue")
        Double totalRevenue;
        
        @JsonProperty("activeCourtGroups")
        Long activeCourtGroups;
        
        @JsonProperty("averageRating")
        Double averageRating;
    }

    @Value
    @Builder
    @Jacksonized
    public static class BookingStats {
        @JsonProperty("byStatus")
        Map<String, Long> byStatus;
        
        @JsonProperty("byTimeSlot")
        List<TimeSlotStat> byTimeSlot;
    }

    @Value
    @Builder
    @Jacksonized
    public static class TimeSlotStat {
        @JsonProperty("timeSlotId")
        Long timeSlotId;
        
        @JsonProperty("timeSlotName")
        String timeSlotName;
        
        @JsonProperty("count")
        Long count;
        
        @JsonProperty("revenue")
        Double revenue;
    }

    @Value
    @Builder
    @Jacksonized
    public static class RevenueChartPoint {
        String date;
        Double revenue;
        Long bookings;
    }

    @Value
    @Builder
    @Jacksonized
    public static class TopCourtGroup {
        Long id;
        String name;
        String address;
        String type;
        
        @JsonProperty("bookingCount")
        Long bookingCount;
        
        Double revenue;
        Double rating;
    }

    @Value
    @Builder
    @Jacksonized
    public static class UserStats {
        @JsonProperty("byRole")
        Map<String, Long> byRole;
        
        @JsonProperty("byOwnerStatus")
        Map<String, Long> byOwnerStatus;
    }

    @Value
    @Builder
    @Jacksonized
    public static class CourtGroupStats {
        @JsonProperty("byStatus")
        Map<String, Long> byStatus;
        
        @JsonProperty("byType")
        List<TypeStat> byType;
    }

    @Value
    @Builder
    @Jacksonized
    public static class TypeStat {
        String type;
        Long count;
    }
}



