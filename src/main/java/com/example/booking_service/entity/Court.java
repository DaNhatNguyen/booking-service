package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "courts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "court_group_id")
    Long courtGroupId;

    String name;

    @Column(name = "is_active")
    Integer isActive;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}