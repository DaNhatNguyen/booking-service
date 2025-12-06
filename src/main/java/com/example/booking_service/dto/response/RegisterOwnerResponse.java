package com.example.booking_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO for Owner registration response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterOwnerResponse {
    Long id;
    String fullName;
    String email;
    String phone;
    String role;
    String ownerStatus;
    String createdAt;
    String message;
}




















