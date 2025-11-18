package com.example.booking_service.dto.request;

import com.example.booking_service.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    String fullName;
    String email;
    String password;
    String phone;
    String avatar;

    Role role;
}
