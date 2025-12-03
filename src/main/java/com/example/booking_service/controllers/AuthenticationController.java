package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.AuthenticationRequest;
import com.example.booking_service.dto.request.IntrospectRequest;
import com.example.booking_service.dto.request.LogoutRequest;
import com.example.booking_service.dto.request.RegisterOwnerRequest;
import com.example.booking_service.dto.request.RegisterRequest;
import com.example.booking_service.dto.response.AuthenticationResponse;
import com.example.booking_service.dto.response.IntrospectResponse;
import com.example.booking_service.dto.response.RegisterOwnerResponse;
import com.example.booking_service.dto.response.RegisterResponse;
import com.example.booking_service.dto.response.UserResponse;
import com.example.booking_service.service.AuthenticationService;
import com.example.booking_service.service.UserService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    UserService userService;

    @PostMapping("/login")
    AuthenticationResponse login(@RequestBody AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);

        return result;
    }

    @PostMapping("/register")
    RegisterResponse register(@RequestBody RegisterRequest request) {
        var result = authenticationService.register(request);

        return result;
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);

        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);

        return ApiResponse.<Void>builder()
                .build();
    }

    /**
     * Register new owner account
     * POST /api/auth/register-owner
     * Content-Type: multipart/form-data
     * 
     * @param fullName Owner full name
     * @param email Owner email (must be unique)
     * @param password Password (will be hashed)
     * @param phone Phone number
     * @param bankName Bank name
     * @param bankAccountNumber Bank account number
     * @param bankAccountName Bank account holder name
     * @param idCardFront ID card front image (required)
     * @param idCardBack ID card back image (required)
     * @param bankQrImage Bank QR code image (optional)
     * @return RegisterOwnerResponse with PENDING status
     */
    @PostMapping("/register-owner")
    public ResponseEntity<ApiResponse<RegisterOwnerResponse>> registerOwner(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phone") String phone,
            @RequestParam("bankName") String bankName,
            @RequestParam("bankAccountNumber") String bankAccountNumber,
            @RequestParam("bankAccountName") String bankAccountName,
            @RequestParam("idCardFront") MultipartFile idCardFront,
            @RequestParam("idCardBack") MultipartFile idCardBack,
            @RequestParam(value = "bankQrImage", required = false) MultipartFile bankQrImage) {
        
        // Build request object
        RegisterOwnerRequest request = RegisterOwnerRequest.builder()
                .fullName(fullName)
                .email(email)
                .password(password)
                .phone(phone)
                .bankName(bankName)
                .bankAccountNumber(bankAccountNumber)
                .bankAccountName(bankAccountName)
                .build();
        
        // Call service
        RegisterOwnerResponse result = authenticationService.registerOwner(
                request, idCardFront, idCardBack, bankQrImage);
        
        // Return response with 201 Created status
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<RegisterOwnerResponse>builder()
                        .message(result.getMessage())
                        .result(result)
                        .build());
    }
    
    /**
     * Get current user information
     * GET /api/auth/myInfo
     */
    @GetMapping("/myInfo")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

}
