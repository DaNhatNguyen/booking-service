package com.example.booking_service.repository.projection;

import java.time.LocalDate;
import java.time.LocalTime;

public interface RecentBookingProjection {
    Long getBookingId();
    String getUserName();
    String getCourtGroupName();
    String getCourtName();
    LocalDate getBookingDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    String getStatus();
    Double getPrice();
}


