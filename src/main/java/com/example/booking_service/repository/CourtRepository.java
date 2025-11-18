package com.example.booking_service.repository;

import com.example.booking_service.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByCourtGroupId(Long courtGroupId);

    // @Query("""
    // SELECT c.id AS courtId, c.name AS courtName,
    //        ts.id AS slotId, ts.startTime, ts.endTime,
    //        cp.price AS price
    // FROM Court c
    // LEFT JOIN CourtPrice cp ON cp.court.id = c.id 
    //     AND cp.timeSlot.id = ts.id
    //     AND cp.dayType = :dayType
    //     AND cp.effectiveDate <= :bookingDate
    // JOIN TimeSlot ts
    // WHERE c.courtGroup.id = :court KristinaGroupId
    //   AND (cp.effectiveDate IS NULL OR cp.effectiveDate = (
    //       SELECT MAX(cp2.effectiveDate) 
    //       FROM CourtPrice cp2 
    //       WHERE cp2.court.id = c.id 
    //         AND cp2.timeSlot.id = ts.id 
    //         AND cp2.dayType = :dayType
    //         AND cp2.effectiveDate <= :bookingDate
    //   ))
    // ORDER BY c.id, ts.id
    // """)
    // List<ICourtSlotPriceProjection> findAllSlotsWithPrice(
    //         @Param("courtGroupId") Long courtGroupId,
    //         @Param("bookingDate") LocalDate bookingDate,
    //         @Param("dayType") String dayType
    // );

    // @Query("""
    // SELECT b.id AS bookingId, c.id AS courtId, 
    //        ts.id AS slotId, ts.startTime, ts.endTime,
    //        b.totalPrice
    // FROM Booking b
    // JOIN b.bookingSlots bs
    // JOIN bs.timeSlot ts
    // JOIN b.court c
    // WHERE c.courtGroup.id = :courtGroupId
    //   AND b.bookingDate = :bookingDate
    //   AND b.status = 'confirmed'
    // ORDER BY c.id, ts.id
    // """)
    // List<IBookingSlotProjection> findBookingsByCourtGroupAndDate(
    //         @Param("courtGroupId") Long courtGroupId,
    //         @Param("bookingDate") LocalDate bookingDate
    // );

    // // Projection interface
    // public interface ICourtSlotPriceProjection {
    //     Long getCourtId();
    //     String getCourtName();
    //     Long getSlotId();
    //     LocalTime getStartTime();
    //     LocalTime getEndTime();
    //     Double getPrice();
    // }

    // public interface IBookingSlotProjection {
    //     Long getBookingId();
    //     Long getCourtId();
    //     Long getSlotId();
    //     LocalTime getStartTime();
    //     LocalTime getEndTime();
    //     Double getTotalPrice();
    //     LocalDate getBookingDate();
    // }
}