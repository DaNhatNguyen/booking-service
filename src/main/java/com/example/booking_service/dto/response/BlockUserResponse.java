package com.example.booking_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockUserResponse {
    Long id;
    Integer isBlock; // 1 = blocked, 0 = active
}

