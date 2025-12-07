package com.example.booking_service.enums;

/**
 * Enum representing the status of an owner application
 * 
 * PENDING: Application submitted, waiting for admin review
 * APPROVED: Application approved, owner can login and manage courts
 * REJECTED: Application rejected by admin
 * BANNED: Owner account has been banned
 */
public enum OwnerStatus {
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt
    REJECTED,   // Từ chối
    BANNED      // Bị cấm
}






















