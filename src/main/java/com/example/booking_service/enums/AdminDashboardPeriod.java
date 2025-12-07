package com.example.booking_service.enums;

import java.util.Arrays;

public enum AdminDashboardPeriod {
    DAYS_7("7d", 7),
    DAYS_14("14d", 14),
    DAYS_30("30d", 30);

    private final String value;
    private final int days;

    AdminDashboardPeriod(String value, int days) {
        this.value = value;
        this.days = days;
    }

    public String getValue() {
        return value;
    }

    public int getDays() {
        return days;
    }

    public static AdminDashboardPeriod fromValue(String value) {
        return Arrays.stream(values())
                .filter(period -> period.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid period value: " + value));
    }
}




















