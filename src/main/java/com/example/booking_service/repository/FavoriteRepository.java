package com.example.booking_service.repository;

import com.example.booking_service.entity.Favorite;
import com.example.booking_service.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.userId = :userId")
    long countFavoritesByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}

