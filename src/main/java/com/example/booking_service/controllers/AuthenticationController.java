package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.*;
import com.example.booking_service.dto.response.AuthenticationResponse;
import com.example.booking_service.dto.response.IntrospectResponse;
import com.example.booking_service.dto.response.RegisterResponse;
import com.example.booking_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

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


}
