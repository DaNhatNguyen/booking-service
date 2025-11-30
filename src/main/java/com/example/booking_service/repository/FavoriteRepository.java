package com.example.booking_service.repository;

import com.example.booking_service.entity.Favorite;
import com.example.booking_service.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.userId = :userId")
    long countFavoritesByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndCourtGroupId(Long userId, Long courtGroupId);
    
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId AND f.courtGroupId = :courtGroupId")
    void deleteByUserIdAndCourtGroupId(@Param("userId") Long userId, @Param("courtGroupId") Long courtGroupId);
    
    List<Favorite> findByUserId(Long userId);
}

