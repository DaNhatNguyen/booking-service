package com.example.booking_service.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdationRequest {
    String password;
    String firstName;
    String lastName;
    LocalDate dob;
}
