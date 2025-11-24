package com.example.booking_service.service;

import com.example.booking_service.dto.request.CreateReviewRequest;
import com.example.booking_service.dto.response.ReviewResponse;
import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.CourtGroup;
import com.example.booking_service.entity.Review;
import com.example.booking_service.entity.User;
import com.example.booking_service.exception.AppException;
import com.example.booking_service.exception.ErrorCode;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.ReviewRepository;
import com.example.booking_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewService {

    ReviewRepository reviewRepository;
    BookingRepository bookingRepository;
    CourtGroupRepository courtGroupRepository;
    UserRepository userRepository;

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<ReviewResponse> getReviewsByCourtGroup(Long courtGroupId) {
        return reviewRepository.findByCourtGroupId(courtGroupId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(String userEmail, CreateReviewRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(ErrorCode.INVALID_RATING);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_EXISTED));

        if (!booking.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        if (reviewRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        CourtGroup courtGroup = courtGroupRepository.findById(request.getCourtGroupId())
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));

        Review review = Review.builder()
                .userId(user.getId())
                .courtGroupId(courtGroup.getId())
                .bookingId(booking.getId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        // cập nhật rating trung bình cho court group
        updateCourtGroupRating(courtGroup);

        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse createReviewForCourtGroup(String userEmail, Long courtGroupId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new AppException(ErrorCode.INVALID_RATING);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        CourtGroup courtGroup = courtGroupRepository.findById(courtGroupId)
                .orElseThrow(() -> new AppException(ErrorCode.COURT_GROUP_NOT_EXISTED));

        List<Booking> bookings = bookingRepository
                .findConfirmedBookingsForUserAndCourtGroup(user.getId(), courtGroupId);

        if (bookings.isEmpty()) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // Lấy booking mới nhất (đã được order trong query)
        Booking booking = bookings.get(0);

        if (reviewRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.builder()
                .userId(user.getId())
                .courtGroupId(courtGroup.getId())
                .bookingId(booking.getId())
                .rating(rating)
                .comment(comment)
                .build();

        Review saved = reviewRepository.save(review);

        updateCourtGroupRating(courtGroup);

        return toResponse(saved);
    }

    private void updateCourtGroupRating(CourtGroup courtGroup) {
        List<Review> reviews = reviewRepository.findByCourtGroupId(courtGroup.getId());
        if (reviews.isEmpty()) {
            courtGroup.setRating(null);
        } else {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            courtGroup.setRating(avg);
        }
        courtGroupRepository.save(courtGroup);
    }

    private ReviewResponse toResponse(Review review) {
        User user = userRepository.findById(review.getUserId()).orElse(null);

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .courtGroupId(review.getCourtGroupId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                .build();
    }
}


