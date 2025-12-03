package com.example.booking_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockUserRequest {
    
    @JsonProperty("is_block")
    @NotNull(message = "INVALID_KEY")
    Integer isBlock; // 1 = khóa, 0 = mở khóa
}

