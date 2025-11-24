package com.example.booking_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserDetailResponse {
    Long id;
    
    @JsonProperty("full_name")
    String fullName;
    
    String email;
    String phone;
    String avatar;
    String role;
    
    @JsonProperty("owner_status")
    String ownerStatus;
    
    @JsonProperty("created_at")
    String createdAt;
    
    @JsonProperty("updated_at")
    String updatedAt;
    
    @JsonProperty("id_card_front")
    String idCardFront;
    
    @JsonProperty("id_card_back")
    String idCardBack;
    
    @JsonProperty("business_license")
    String businessLicense;
    
    @JsonProperty("owner_verified_at")
    String ownerVerifiedAt;
    
    @JsonProperty("bank_qr_image")
    String bankQrImage;
    
    @JsonProperty("bank_name")
    String bankName;
    
    @JsonProperty("bank_account_number")
    String bankAccountNumber;
    
    @JsonProperty("bank_account_name")
    String bankAccountName;
    
    UserStatisticsResponse statistics;
}




