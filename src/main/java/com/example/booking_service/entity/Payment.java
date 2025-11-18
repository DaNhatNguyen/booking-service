package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "booking_id")
    Long bookingId;

    @Column(name = "payment_method")
    String paymentMethod;

    String status;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    Double amount;
}