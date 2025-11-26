package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardResponse {
    OverviewMetrics overview;
    
    @JsonProperty("bookingTrend")
    List<SeriesPoint> bookingTrend;
    
    @JsonProperty("revenueTrend")
    List<SeriesPoint> revenueTrend;
    
    @JsonProperty("bookingStatusDistribution")
    List<StatusDistribution> bookingStatusDistribution;
    
    @JsonProperty("topCourtGroups")
    List<CourtGroupHighlight> topCourtGroups;
    
    @JsonProperty("recentBookings")
    List<BookingRow> recentBookings;
    
    @JsonProperty("reviewHighlights")
    List<ReviewHighlight> reviewHighlights;
    
    @JsonProperty("utilization")
    List<UtilizationBreakdown> utilization;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewMetrics {
        @JsonProperty("totalCourtGroups")
        Integer totalCourtGroups;
        
        @JsonProperty("totalBookings")
        Integer totalBookings;
        
        @JsonProperty("totalRevenue")
        Double totalRevenue;
        
        @JsonProperty("averageRating")
        Double averageRating;
        
        @JsonProperty("pendingBookings")
        Integer pendingBookings;
        
        @JsonProperty("confirmedBookings")
        Integer confirmedBookings;
        
        @JsonProperty("cancelledBookings")
        Integer cancelledBookings;
        
        @JsonProperty("bookingGrowth")
        Integer bookingGrowth;
        
        @JsonProperty("revenueGrowth")
        Integer revenueGrowth;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesPoint {
        String label;
        Integer bookings;
        Double revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistribution {
        String label;
        Integer value;
        String color;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtGroupHighlight {
        Long id;
        String name;
        String address;
        String district;
        String type;
        Double rating;
        Integer bookings;
        Double revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRow {
        Long id;
        
        @JsonProperty("userName")
        String userName;
        
        @JsonProperty("courtName")
        String courtName;
        
        @JsonProperty("bookingDate")
        String bookingDate;
        
        String time;
        String status;
        Double price;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewHighlight {
        Long id;
        
        @JsonProperty("userName")
        String userName;
        
        @JsonProperty("courtGroup")
        String courtGroup;
        
        Integer rating;
        String comment;
        
        @JsonProperty("createdAt")
        String createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtilizationBreakdown {
        @JsonProperty("timeSlotId")
        Integer timeSlotId;
        
        String label;
        Integer value;
        String color;
    }
}

