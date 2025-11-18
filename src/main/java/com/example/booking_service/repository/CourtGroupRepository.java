package com.example.booking_service.repository;

import com.example.booking_service.entity.CourtGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtGroupRepository extends JpaRepository<CourtGroup, Long> {

    List<CourtGroup> findByProvinceAndDistrict(String province, String district);
}


