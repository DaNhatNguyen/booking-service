package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "court_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourtPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "court_group_id")
    Long courtGroupId;

    @Column(name = "court_id")
    Long courtId;

    @Column(name = "time_slot_id")
    Long timeSlotId;

    @Column(name = "day_type")
    String dayType;

    Double price;

    @Column(name = "effective_date")
    LocalDate effectiveDate;
}