package com.example.booking_service.repository;

import com.example.booking_service.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.courtId = :courtId AND b.bookingDate = :date AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByCourtAndDate(@Param("courtId") Long courtId,
                                                   @Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds AND b.bookingDate = :date AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByCourtIdsAndDate(@Param("courtIds") List<Long> courtIds,
                                                      @Param("date") LocalDate date);

    List<Booking> findByUserId(Long userId);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.userId = :userId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.courtId IN (SELECT c.id FROM Court c WHERE c.courtGroupId = :courtGroupId) " +
            "ORDER BY b.bookingDate DESC, b.createdAt DESC")
    List<Booking> findConfirmedBookingsForUserAndCourtGroup(@Param("userId") Long userId,
                                                            @Param("courtGroupId") Long courtGroupId);
}