package com.example.booking_service.exception;

import com.example.booking_service.dto.request.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// khi có exception thì class này sẽ chịu trách nhiệm handle việc đó
@ControllerAdvice
public class GlobalExceptionHandler {

    // ngoại lệ không chưa bắt được
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception) {

//        ApiResponse apiResponse = new ApiResponse<>();
//
//        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
//        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build());
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
//        ApiResponse apiResponse = new ApiResponse<>();
//
//        apiResponse.setCode(1001);
//        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .code(1001)
                .message(exception.getMessage())
                .build());
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
//        ApiResponse apiResponse = new ApiResponse<>();
//
//        apiResponse.setCode(errorCode.getCode());
//        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build());
    }

    // khi validate truong gui len
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
//            throw new RuntimeException(e);
        }

//        ApiResponse apiResponse = new ApiResponse();
//        apiResponse.setCode(errorCode.getCode());
//        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build());
    }
}
