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

    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE cg.province = :province AND cg.district = :district " +
            "AND cg.status = 'approved' AND (cg.isDeleted = false OR cg.isDeleted IS NULL)")
    List<CourtGroup> findByProvinceAndDistrict(@Param("province") String province, 
                                                @Param("district") String district);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE cg.type = :type AND cg.province = :province AND cg.district = :district " +
            "AND (cg.isDeleted = false OR cg.isDeleted IS NULL)")
    List<CourtGroup> findByTypeAndProvinceAndDistrict(@Param("type") String type, 
                                                        @Param("province") String province, 
                                                        @Param("district") String district);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE cg.ownerId = :ownerId " +
            "AND (cg.isDeleted = false OR cg.isDeleted IS NULL)")
    List<CourtGroup> findByOwnerId(@Param("ownerId") Long ownerId);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE cg.ownerId = :ownerId AND cg.status = :status " +
            "AND (cg.isDeleted = false OR cg.isDeleted IS NULL)")
    List<CourtGroup> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, 
                                             @Param("status") String status);
    
    long countByStatus(String status);
    
    long countByOwnerIdAndStatus(Long ownerId, String status);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE (:status IS NULL OR cg.status = :status) " +
            "AND (cg.isDeleted = false OR cg.isDeleted IS NULL) " +
            "ORDER BY cg.createdAt DESC")
    Page<CourtGroup> findAllWithFilters(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT cg FROM CourtGroup cg " +
            "WHERE (cg.isDeleted = false OR cg.isDeleted IS NULL) " +
            "AND (cg.status = 'approved' OR cg.status IS NULL) " +
            "AND cg.rating > 0 " +
            "ORDER BY cg.rating DESC")
    Page<CourtGroup> findTopRatedCourtGroups(Pageable pageable);
}


