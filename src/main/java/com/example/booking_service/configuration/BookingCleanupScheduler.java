package com.example.booking_service.configuration;

import com.example.booking_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled job to automatically clean up expired bookings
 * Runs every 1 minute to delete bookings with:
 * - status = 'PAYING'
 * - created_at < (current time - 5 minutes)
 * 
 * This is CRITICAL to handle cases where users:
 * - Close the browser tab
 * - Navigate away from payment page
 * - Lose network connection
 * 
 * Without this scheduler, expired bookings would remain in the database
 * and continue to occupy time slots.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingCleanupScheduler {

    BookingService bookingService;

    /**
     * Scheduled task that runs every 1 minute (60000ms)
     * Deletes all bookings that:
     * 1. Have status = 'PAYING'
     * 2. Were created more than 5 minutes ago
     */
    @Scheduled(fixedRate = 60000) // Run every 1 minute
    public void cleanupExpiredBookings() {
        try {
            log.debug("Running scheduled cleanup of expired bookings...");
            
            int deletedCount = bookingService.deleteExpiredBookings();
            
            if (deletedCount > 0) {
                log.info("✓ Cleaned up {} expired booking(s) with PAYING status", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("✗ Error during scheduled cleanup of expired bookings", e);
        }
    }
}




