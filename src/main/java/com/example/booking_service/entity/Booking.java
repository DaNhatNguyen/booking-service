package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(name = "start_time")
    LocalTime startTime;

    @Column(name = "end_time")
    LocalTime endTime;

    String status;

    Double price;

    String address;

    @Column(name = "payment_proof")
    String paymentProof;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}