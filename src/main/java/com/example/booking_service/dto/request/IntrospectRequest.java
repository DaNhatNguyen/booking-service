package com.example.booking_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor // sinh ra constructor no tham so
@AllArgsConstructor // full tham so
@Builder // tạo object và get, set nhanh hon
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntrospectRequest {
    String token;
}
