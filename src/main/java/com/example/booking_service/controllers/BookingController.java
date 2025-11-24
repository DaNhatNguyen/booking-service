package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateBookingRequest;
import com.example.booking_service.dto.request.UpdateBookingStatusRequest;
import com.example.booking_service.dto.response.*;
import com.example.booking_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {

    BookingService bookingService;

    @GetMapping
    public ApiResponse<BookingListResponse> getBookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate booking_date,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long owner_id) {
        
        BookingListResponse response = bookingService.getBookings(
                page, limit, status, booking_date, search, owner_id);
        
        return ApiResponse.<BookingListResponse>builder()
                .message("Success")
                .result(response)
                .build();
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingDetailResponse> getBookingById(@PathVariable Long bookingId) {
        BookingDetailResponse response = bookingService.getBookingById(bookingId);
        return ApiResponse.<BookingDetailResponse>builder()
                .message("Success")
                .result(response)
                .build();
    }

    @PutMapping("/{bookingId}/status")
    public ApiResponse<UpdateBookingStatusResponse> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestBody UpdateBookingStatusRequest request) {
        UpdateBookingStatusResponse response = bookingService.updateBookingStatus(bookingId, request.getStatus());
        return ApiResponse.<UpdateBookingStatusResponse>builder()
                .message("Đã cập nhật trạng thái booking")
                .result(response)
                .build();
    }

    @DeleteMapping("/{bookingId}")
    public ApiResponse<Void> deleteBooking(@PathVariable Long bookingId) {
        bookingService.deleteBooking(bookingId);
        return ApiResponse.<Void>builder()
                .message("Đã xóa booking thành công")
                .build();
    }

    @GetMapping("/{courtGroupId}/data")
    public ApiResponse<BookingByDateResponse> getBookingDataByDate(
            @PathVariable Long courtGroupId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") String date) {
        LocalDate bookingDate = LocalDate.parse(date);
        BookingByDateResponse response = bookingService.getBookingDataByDate(courtGroupId, bookingDate);
        return ApiResponse.<BookingByDateResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/confirmation")
    public ApiResponse<CreateBookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
        CreateBookingResponse response = bookingService.createBooking(request);
        return ApiResponse.<CreateBookingResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<UserBookingHistoryResponse>> getUserBookings(@PathVariable Long userId) {
        List<UserBookingHistoryResponse> bookings = bookingService.getBookingsByUserId(userId);
        return ApiResponse.<List<UserBookingHistoryResponse>>builder()
                .result(bookings)
                .build();
    }

    @PostMapping("/{bookingId}/confirm")
    public ApiResponse<CreateBookingResponse> confirmBooking(@PathVariable Long bookingId) {
        CreateBookingResponse response = bookingService.confirmBooking(bookingId);
        return ApiResponse.<CreateBookingResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Payment API 1: Get payment information for a booking
     * GET /api/bookings/{bookingId}/payment-info
     */
    @GetMapping("/{bookingId}/payment-info")
    public ApiResponse<PaymentInfoResponse> getPaymentInfo(@PathVariable Long bookingId) {
        PaymentInfoResponse response = bookingService.getPaymentInfo(bookingId);
        return ApiResponse.<PaymentInfoResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Payment API 2: Confirm payment with proof image upload
     * POST /api/bookings/{bookingId}/confirm-payment
     */
    @PostMapping("/{bookingId}/confirm-payment")
    public ApiResponse<ConfirmPaymentResponse> confirmPayment(
            @PathVariable Long bookingId,
            @RequestParam("payment_proof") MultipartFile paymentProof) {
        ConfirmPaymentResponse response = bookingService.confirmPayment(bookingId, paymentProof);
        return ApiResponse.<ConfirmPaymentResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Payment API 3: Cancel expired booking (optional - for manual call)
     * DELETE /api/bookings/{bookingId}/cancel-expired
     */
    @DeleteMapping("/{bookingId}/cancel-expired")
    public ApiResponse<CancelExpiredResponse> cancelExpiredBooking(@PathVariable Long bookingId) {
        CancelExpiredResponse response = bookingService.cancelExpiredBooking(bookingId);
        return ApiResponse.<CancelExpiredResponse>builder()
                .result(response)
                .build();
    }
}