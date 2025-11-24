package com.example.booking_service.repository;

import com.example.booking_service.entity.CourtGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtGroupRepository extends JpaRepository<CourtGroup, Long> {

    List<CourtGroup> findByProvinceAndDistrict(String province, String district);
    
    List<CourtGroup> findByTypeAndProvinceAndDistrict(String type, String province, String district);
    
    List<CourtGroup> findByOwnerId(Long ownerId);
    
    List<CourtGroup> findByOwnerIdAndStatus(Long ownerId, String status);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE (:status IS NULL OR cg.status = :status) " +
            "ORDER BY cg.createdAt DESC")
    Page<CourtGroup> findAllWithFilters(@Param("status") String status, Pageable pageable);
}


