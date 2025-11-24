package com.example.booking_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO for Owner registration request
 * Used with multipart/form-data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterOwnerRequest {
    
    // Personal Information
    String fullName;
    String email;
    String password;
    String phone;
    
    // Bank Information
    String bankName;
    String bankAccountNumber;
    String bankAccountName;
    
    // File uploads (handled separately in controller as MultipartFile)
    // These fields are not part of this DTO
    // - idCardFront: MultipartFile
    // - idCardBack: MultipartFile
    // - bankQrImage: MultipartFile (optional)
}

