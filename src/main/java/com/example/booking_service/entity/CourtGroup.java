package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "court_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourtGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "owner_id")
    Long ownerId;

    String name;
    String type;
    String address;
    String district;
    String province;

    @Column(name = "phone")
    String phoneNumber;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "image", columnDefinition = "TEXT")
    String image;

    Double rating;

    @Column(name = "open_time")
    LocalTime openTime;

    @Column(name = "close_time")
    LocalTime closeTime;

    @Column(name = "status")
    String status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "is_deleted")
    Boolean isDeleted;

}



