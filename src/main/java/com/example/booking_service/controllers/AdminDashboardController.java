package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.response.AdminDashboardResponse;
import com.example.booking_service.entity.User;
import com.example.booking_service.enums.AdminDashboardPeriod;
import com.example.booking_service.enums.Role;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.UserRepository;
import com.example.booking_service.service.AdminDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardController {

    AdminDashboardService adminDashboardService;
    UserRepository userRepository;

    @GetMapping("/overview")
    public ApiResponse<AdminDashboardResponse> getDashboardOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String period) {

        checkAdminRole();

        if (startDate == null || endDate == null) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        if (endDate.isBefore(startDate)) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        AdminDashboardPeriod dashboardPeriod;
        try {
            dashboardPeriod = AdminDashboardPeriod.fromValue(period);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid period value provided: {}", period);
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        var response = adminDashboardService.getDashboardOverview(startDate, endDate, dashboardPeriod);
        return ApiResponse.<AdminDashboardResponse>builder()
                .result(response)
                .build();
    }

    private void checkAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}


















