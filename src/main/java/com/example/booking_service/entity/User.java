package com.example.booking_service.entity;

import com.example.booking_service.enums.OwnerStatus;
import com.example.booking_service.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "full_name")
    String fullName;

    String email;
    String password;
    String phone;
    String avatar;

    @Enumerated(EnumType.STRING) // ánh xạ giá trị ENUM sang text trong DB
    Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_status")
    OwnerStatus ownerStatus;

    @Column(name = "id_card_front")
    String idCardFront;

    @Column(name = "id_card_back")
    String idCardBack;

    @Column(name = "bank_qr_image")
    String bankQrImage;

    @Column(name = "bank_name", length = 100)
    String bankName;

    @Column(name = "bank_account_number", length = 50)
    String bankAccountNumber;

    @Column(name = "bank_account_name")
    String bankAccountName;

    @Column(name = "owner_verified_at")
    LocalDateTime ownerVerifiedAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
