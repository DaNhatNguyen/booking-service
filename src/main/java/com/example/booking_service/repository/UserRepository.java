package com.example.booking_service.repository;

import com.example.booking_service.entity.User;
import com.example.booking_service.enums.OwnerStatus;
import com.example.booking_service.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    
    Optional<User> findByIdAndRole(Long id, Role role);
    
    long countByRole(Role role);
    
    long countByRoleAndOwnerStatus(Role role, OwnerStatus ownerStatus);
    
    @Query("SELECT u FROM User u " +
            "WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:ownerStatus IS NULL OR u.ownerStatus = :ownerStatus) " +
            "AND (:search IS NULL OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.bankName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.bankAccountName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.bankAccountNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> findUsersWithFilters(@Param("role") Role role,
                                     @Param("ownerStatus") OwnerStatus ownerStatus,
                                     @Param("search") String search,
                                     Pageable pageable);
    
    // Statistics queries
    @Query(value = "SELECT u.role, COUNT(*) FROM users u GROUP BY u.role", nativeQuery = true)
    List<Object[]> countUsersByRole();
    
    @Query(value = "SELECT u.owner_status, COUNT(*) FROM users u WHERE u.role = 'OWNER' GROUP BY u.owner_status", nativeQuery = true)
    List<Object[]> countOwnersByStatus();
}
