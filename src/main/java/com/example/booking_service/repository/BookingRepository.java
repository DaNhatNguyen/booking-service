package com.example.booking_service.repository;

import com.example.booking_service.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.courtId = :courtId AND b.bookingDate = :date AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByCourtAndDate(@Param("courtId") Long courtId,
                                                   @Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds AND b.bookingDate = :date AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByCourtIdsAndDate(@Param("courtIds") List<Long> courtIds,
                                                      @Param("date") LocalDate date);

    List<Booking> findByUserId(Long userId);
    
    @Query(value = "SELECT b.*, " +
            "u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone, " +
            "c.name AS court_name, " +
            "cg.id AS court_group_id, cg.name AS court_group_name, cg.type AS court_group_type, " +
            "CONCAT(cg.address, ', ', cg.district, ', ', cg.province) AS court_group_address " +
            "FROM bookings b " +
            "JOIN users u ON b.user_id = u.id " +
            "JOIN courts c ON b.court_id = c.id " +
            "JOIN court_groups cg ON c.court_group_id = cg.id " +
            "WHERE (:status IS NULL OR b.status = :status) " +
            "AND (:bookingDate IS NULL OR b.booking_date = :bookingDate) " +
            "AND (:search IS NULL OR u.full_name LIKE %:search% OR u.email LIKE %:search% OR u.phone LIKE %:search%) " +
            "AND (:ownerId IS NULL OR cg.owner_id = :ownerId) " +
            "ORDER BY b.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM bookings b " +
            "JOIN users u ON b.user_id = u.id " +
            "JOIN courts c ON b.court_id = c.id " +
            "JOIN court_groups cg ON c.court_group_id = cg.id " +
            "WHERE (:status IS NULL OR b.status = :status) " +
            "AND (:bookingDate IS NULL OR b.booking_date = :bookingDate) " +
            "AND (:search IS NULL OR u.full_name LIKE %:search% OR u.email LIKE %:search% OR u.phone LIKE %:search%) " +
            "AND (:ownerId IS NULL OR cg.owner_id = :ownerId)",
            nativeQuery = true)
    Page<Booking> findBookingsWithFilters(
            @Param("status") String status,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("search") String search,
            @Param("ownerId") Long ownerId,
            Pageable pageable);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.userId = :userId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.courtId IN (SELECT c.id FROM Court c WHERE c.courtGroupId = :courtGroupId) " +
            "ORDER BY b.bookingDate DESC, b.createdAt DESC")
    List<Booking> findConfirmedBookingsForUserAndCourtGroup(@Param("userId") Long userId,
                                                            @Param("courtGroupId") Long courtGroupId);
    
    @Query("SELECT COUNT(b) FROM Booking b " +
            "JOIN Court c ON b.courtId = c.id " +
            "WHERE c.courtGroupId = :courtGroupId " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND b.bookingDate >= CURRENT_DATE")
    long countActiveBookingsByCourtGroupId(@Param("courtGroupId") Long courtGroupId);
    
    @Query("SELECT COUNT(b) FROM Booking b " +
            "JOIN Court c ON b.courtId = c.id " +
            "JOIN CourtGroup cg ON c.courtGroupId = cg.id " +
            "WHERE cg.ownerId = :ownerId " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND b.bookingDate >= CURRENT_DATE")
    long countActiveBookingsByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT COUNT(b) FROM Booking b " +
            "JOIN Court c ON b.courtId = c.id " +
            "WHERE c.courtGroupId = :courtGroupId")
    long countTotalBookingsByCourtGroupId(@Param("courtGroupId") Long courtGroupId);
    
    @Query("SELECT COALESCE(SUM(b.price), 0.0) FROM Booking b " +
            "JOIN Court c ON b.courtId = c.id " +
            "WHERE c.courtGroupId = :courtGroupId " +
            "AND b.status IN ('CONFIRMED', 'COMPLETED')")
    Double sumRevenueByCourtGroupId(@Param("courtGroupId") Long courtGroupId);
    
    // User management queries
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.userId = :userId " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND b.bookingDate >= CURRENT_DATE")
    long countActiveBookingsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId")
    long countTotalBookingsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(SUM(b.price), 0.0) FROM Booking b " +
            "WHERE b.userId = :userId " +
            "AND b.status IN ('CONFIRMED', 'COMPLETED')")
    Double sumTotalSpentByUserId(@Param("userId") Long userId);

    // Payment related queries
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.createdAt < :expiryTime")
    List<Booking> findByStatusAndCreatedAtBefore(@Param("status") String status,
                                                 @Param("expiryTime") LocalDateTime expiryTime);
}