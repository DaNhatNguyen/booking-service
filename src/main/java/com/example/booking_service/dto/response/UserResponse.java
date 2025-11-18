package com.example.booking_service.dto.response;

import com.example.booking_service.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor // sinh ra constructor no tham so
@AllArgsConstructor // full tham so
@Builder // tạo object và get, set nhanh hon
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String fullName;
    String email;
    String phone;
    String avatar;
    Role role;
}
