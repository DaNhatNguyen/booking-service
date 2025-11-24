package com.example.booking_service.repository;

import com.example.booking_service.entity.User;
import com.example.booking_service.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u " +
            "WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithFilters(@Param("role") Role role, 
                                     @Param("search") String search, 
                                     Pageable pageable);
 }
