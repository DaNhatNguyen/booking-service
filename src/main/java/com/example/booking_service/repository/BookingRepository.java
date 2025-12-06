package com.example.booking_service.repository;

import com.example.booking_service.entity.Booking;
import com.example.booking_service.repository.projection.RecentBookingProjection;
import com.example.booking_service.repository.projection.TopCourtGroupProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalTime;
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
            "AND (:courtGroupId IS NULL OR cg.id = :courtGroupId) " +
            "ORDER BY b.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM bookings b " +
            "JOIN users u ON b.user_id = u.id " +
            "JOIN courts c ON b.court_id = c.id " +
            "JOIN court_groups cg ON c.court_group_id = cg.id " +
            "WHERE (:status IS NULL OR b.status = :status) " +
            "AND (:bookingDate IS NULL OR b.booking_date = :bookingDate) " +
            "AND (:search IS NULL OR u.full_name LIKE %:search% OR u.email LIKE %:search% OR u.phone LIKE %:search%) " +
            "AND (:ownerId IS NULL OR cg.owner_id = :ownerId) " +
            "AND (:courtGroupId IS NULL OR cg.id = :courtGroupId)",
            nativeQuery = true)
    Page<Booking> findBookingsWithFilters(
            @Param("status") String status,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("search") String search,
            @Param("ownerId") Long ownerId,
            @Param("courtGroupId") Long courtGroupId,
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
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate")
    long countByBookingDateBetween(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(b.price), 0) FROM Booking b " +
            "WHERE b.bookingDate BETWEEN :startDate AND :endDate " +
            "AND b.status IN ('PENDING', 'CONFIRMED')")
    Double sumRevenueByBookingDateBetween(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b.bookingDate AS bookingDate, COUNT(b) AS total " +
            "FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate " +
            "GROUP BY b.bookingDate ORDER BY b.bookingDate")
    List<Object[]> countBookingsPerDay(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b.bookingDate AS bookingDate, COALESCE(SUM(b.price), 0) AS total " +
            "FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "GROUP BY b.bookingDate ORDER BY b.bookingDate")
    List<Object[]> sumRevenuePerDay(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b.status AS status, COUNT(b) AS total " +
            "FROM Booking b WHERE b.bookingDate BETWEEN :startDate AND :endDate " +
            "GROUP BY b.status")
    List<Object[]> countBookingsByStatus(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);
    
    @Query("""
            SELECT cg.id AS courtGroupId,
                   cg.name AS courtGroupName,
                   CONCAT(cg.address, ', ', cg.district, ', ', cg.province) AS address,
                   cg.district AS district,
                   cg.type AS type,
                   cg.rating AS rating,
                   COUNT(b.id) AS bookings,
                   COALESCE(SUM(b.price), 0) AS revenue
            FROM Booking b
            JOIN Court c ON b.courtId = c.id
            JOIN CourtGroup cg ON c.courtGroupId = cg.id
            WHERE b.bookingDate BETWEEN :startDate AND :endDate
            GROUP BY cg.id, cg.name, cg.address, cg.district, cg.province, cg.type, cg.rating
            ORDER BY revenue DESC
            """)
    Page<TopCourtGroupProjection> findTopCourtGroups(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
    
    @Query("""
            SELECT b.id AS bookingId,
                   u.fullName AS userName,
                   cg.name AS courtGroupName,
                   c.name AS courtName,
                   b.bookingDate AS bookingDate,
                   b.startTime AS startTime,
                   b.endTime AS endTime,
                   b.status AS status,
                   b.price AS price
            FROM Booking b
            JOIN User u ON b.userId = u.id
            JOIN Court c ON b.courtId = c.id
            JOIN CourtGroup cg ON c.courtGroupId = cg.id
            ORDER BY b.createdAt DESC
            """)
    Page<RecentBookingProjection> findRecentBookings(Pageable pageable);
    
    @Query("""
            SELECT COALESCE(b.timeSlotId,
                       CASE 
                           WHEN b.startTime IS NOT NULL AND b.startTime < :eveningStart THEN 1
                           WHEN b.startTime IS NOT NULL THEN 2
                           ELSE 1
                       END) AS slotId,
                   COUNT(b.id) AS total
            FROM Booking b
            WHERE b.bookingDate BETWEEN :startDate AND :endDate
            GROUP BY slotId
            """)
    List<Object[]> countUtilizationByTimeSlot(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate,
                                              @Param("eveningStart") LocalTime eveningStart);
    
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
    
    // Owner dashboard queries
    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds " +
            "AND b.bookingDate BETWEEN :startDate AND :endDate")
    List<Booking> findByCourtIdInAndBookingDateBetween(
            @Param("courtIds") List<Long> courtIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds " +
            "AND b.bookingDate = :date")
    List<Booking> findByCourtIdInAndBookingDate(
            @Param("courtIds") List<Long> courtIds,
            @Param("date") LocalDate date);
    
    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> findByCourtIdInOrderByCreatedAtDesc(
            @Param("courtIds") List<Long> courtIds,
            Pageable pageable);
    
    @Query("SELECT b FROM Booking b WHERE b.courtId IN :courtIds " +
            "AND b.bookingDate BETWEEN :startDate AND :endDate " +
            "AND (b.timeSlotId = :timeSlotId OR " +
            "(b.timeSlotId IS NULL AND b.startTime IS NOT NULL AND " +
            "((:timeSlotId = 1 AND b.startTime < :eveningStart) OR " +
            "(:timeSlotId = 2 AND b.startTime >= :eveningStart))))")
    List<Booking> findByCourtIdInAndBookingDateBetweenAndTimeSlotId(
            @Param("courtIds") List<Long> courtIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("timeSlotId") Long timeSlotId,
            @Param("eveningStart") LocalTime eveningStart);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Check for overlapping bookings on the same court, date, and time
     * Excludes CANCELLED bookings
     * Two bookings overlap if:
     * - booking1.startTime < booking2.endTime AND booking2.startTime < booking1.endTime
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.courtId = :courtId " +
            "AND b.bookingDate = :bookingDate " +
            "AND b.status != 'CANCELLED' " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    boolean hasOverlappingBooking(@Param("courtId") Long courtId,
                                  @Param("bookingDate") LocalDate bookingDate,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);
    
    /**
     * Find overlapping bookings with pessimistic lock for race condition prevention
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.courtId = :courtId " +
            "AND b.bookingDate = :bookingDate " +
            "AND b.status != 'CANCELLED' " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    @Lock(LockModeType.PESSIMISTIC_WRITE) // không cho các bản ghi khác ghi trong lúc kiểm tra
    List<Booking> findOverlappingBookingsWithLock(@Param("courtId") Long courtId,
                                                   @Param("bookingDate") LocalDate bookingDate,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime);
}