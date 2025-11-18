package com.example.booking_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor // sinh ra constructor no tham so
@AllArgsConstructor // full tham so
@Builder // tạo object và get, set nhanh hon
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 3, message = "username must at least 3 character")
    String username;

    @Size(min = 8, message = "PASSWORD_LENGTH")
    String password;
    String firstName;
    String lastName;
    private LocalDate dob;
}
