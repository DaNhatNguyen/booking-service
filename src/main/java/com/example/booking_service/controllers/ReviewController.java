package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.dto.request.CreateReviewRequest;
import com.example.booking_service.dto.response.ReviewResponse;
import com.example.booking_service.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @GetMapping("/{courtGroupId}")
    public ApiResponse<List<ReviewResponse>> getReviews(@PathVariable Long courtGroupId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByCourtGroup(courtGroupId))
                .build();
    }

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(Authentication authentication,
                                                    @RequestBody CreateReviewRequest request) {
        String userEmail = authentication.getName();
        ReviewResponse response = reviewService.createReview(userEmail, request);
        return ApiResponse.<ReviewResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/court-groups/{courtGroupId}")
    public ApiResponse<ReviewResponse> createReviewForCourtGroup(Authentication authentication,
                                                                 @PathVariable Long courtGroupId,
                                                                 @RequestParam Integer rating,
                                                                 @RequestParam(required = false) String comment) {
        String userEmail = authentication.getName();
        ReviewResponse response = reviewService.createReviewForCourtGroup(userEmail, courtGroupId, rating, comment);
        return ApiResponse.<ReviewResponse>builder()
                .result(response)
                .build();
    }
}


