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
    List<CourtPrice> findByCourtId(Long courtId);

    @Query("SELECT cp FROM CourtPrice cp WHERE cp.courtId = :courtId AND " +
            "(cp.effectiveDate IS NULL OR cp.effectiveDate <= :date)")
    List<CourtPrice> findActivePricesByCourtId(@Param("courtId") Long courtId,
                                               @Param("date") LocalDate date);
}