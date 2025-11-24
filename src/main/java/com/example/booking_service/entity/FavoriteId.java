package com.example.booking_service.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for Favorite entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteId implements Serializable {
    private Long userId;
    private Long courtGroupId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoriteId that = (FavoriteId) o;
        return Objects.equals(userId, that.userId) && 
               Objects.equals(courtGroupId, that.courtGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, courtGroupId);
    }
}




