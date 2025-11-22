package com.example.booking_service.repository;

import com.example.booking_service.entity.CourtPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourtPriceRepository extends JpaRepository<CourtPrice, Long> {

    @Query("SELECT cp FROM CourtPrice cp WHERE cp.courtGroupId = :courtGroupId AND " +
            "cp.dayType = :dayType AND (cp.effectiveDate IS NULL OR cp.effectiveDate <= :date) " +
            "ORDER BY cp.effectiveDate DESC")
    List<CourtPrice> findActivePricesByCourtGroupAndDayType(@Param("courtGroupId") Long courtGroupId,
                                                            @Param("dayType") String dayType,
                                                            @Param("date") LocalDate date);

    @Query("SELECT cp FROM CourtPrice cp WHERE cp.courtGroupId = :courtGroupId AND " +
            "cp.dayType = :dayType ORDER BY cp.effectiveDate DESC")
    List<CourtPrice> findLatestPricesByCourtGroupAndDayType(@Param("courtGroupId") Long courtGroupId,
                                                            @Param("dayType") String dayType);
    
    List<CourtPrice> findByCourtGroupId(Long courtGroupId);
    
    CourtPrice findByCourtGroupIdAndTimeSlotIdAndDayType(Long courtGroupId, Long timeSlotId, String dayType);
}