package com.example.booking_service.exception;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(999, "Uncategorized error"),
    INVALID_KEY(1001, "Invalid message key"),
    PASSWORD_LENGTH(1002, "password must at least 8 character"),
    USER_EXISTED(1003, "User existed"),
    USER_NOT_EXISTED(1004, "User not existed"),
    UNAUTHENTICATED(1005, "Unauthenticated"),
    COURT_NOT_EXISTED(1006, "Court not existed"),
    COURT_GROUP_NOT_EXISTED(1007, "Court group not existed")
    ;
    private int code;
    private String message;

    ErrorCode(int code, String message){
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
