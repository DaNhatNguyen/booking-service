package com.example.booking_service.dto.response;

import com.example.booking_service.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterResponse {
    String message;
    User user;
}
