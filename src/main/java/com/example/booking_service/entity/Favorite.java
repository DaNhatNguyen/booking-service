package com.example.booking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@IdClass(FavoriteId.class)
public class Favorite {
    @Id
    @Column(name = "user_id")
    Long userId;

    @Id
    @Column(name = "court_group_id")
    Long courtGroupId;

    @Column(name = "created_at")
    LocalDateTime createdAt;
}