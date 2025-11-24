package com.example.booking_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentInfoResponse {
    Long bookingId;
    String ownerBankName;
    String ownerBankAccountNumber;
    String ownerBankAccountName;
    String ownerBankQrImage;
    Double totalPrice;
    String bookingDate;
    List<TimeSlotInfo> timeSlots;
    String courtName;
    String fullAddress;
    String createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TimeSlotInfo {
        String startTime;
        String endTime;
    }
}



