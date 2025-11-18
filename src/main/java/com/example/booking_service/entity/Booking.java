package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "court_id")
    Long courtId;

    @Column(name = "time_slot_id")
    Long timeSlotId;

    @Column(name = "booking_date")
    LocalDate bookingDate;

    String status;

    Double price;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}