package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL) // neu field null thi khong tra ve
public class ApiResponse<T> {
    @Builder.Default
    int code = 1000;
    String message;
    T result;
}
