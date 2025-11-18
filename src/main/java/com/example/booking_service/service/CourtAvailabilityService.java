package com.example.booking_service.service;

import com.example.booking_service.dto.response.CourtAvailabilityResponse;
import com.example.booking_service.repository.CourtRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtAvailabilityService {

    private final CourtRepository courtRepository;
    private final TimeSlotRepository timeSlotRepository;

    public CourtAvailabilityResponse getAvailability(Long courtGroupId, String dateStr) {
        LocalDate bookingDate = LocalDate.parse(dateStr);
//        String dayType = isWeekend(bookingDate) ? "weekend" : "weekday";
        String dayType = "weekday";

        // 1. Lấy tất cả slot + giá
        var slotPrices = courtRepository.findAllSlotsWithPrice(courtGroupId, bookingDate, dayType);
        // 2. Lấy tất cả booking đã đặt
        var bookedSlots = courtRepository.findBookingsByCourtGroupAndDate(courtGroupId, bookingDate);

        // Group theo court
        Map<Long, CourtAvailabilityResponse.CourtDetail> courtMap = new LinkedHashMap<>();

        // Xử lý prices
        slotPrices.forEach(sp -> {
            courtMap.computeIfAbsent(sp.getCourtId(), id -> CourtAvailabilityResponse.CourtDetail.builder()
                    .id(sp.getCourtId())
                    .name(sp.getCourtName())
                    .bookings(new ArrayList<>())
                    .prices(new ArrayList<>())
                    .build());

            var priceDto = CourtAvailabilityResponse.TimeSlotPrice.builder()
                    .timeSlotId(sp.getSlotId())
                    .startTime(sp.getStartTime())
                    .endTime(sp.getEndTime())
                    .price(sp.getPrice() != null ? sp.getPrice() : 0.0)
                    .build();

            courtMap.get(sp.getCourtId()).getPrices().add(priceDto);
        });

        // Xử lý bookings + gộp slot liền nhau
        bookedSlots.stream()
                .collect(Collectors.groupingBy(CourtRepository.IBookingSlotProjection::getCourtId))
                .forEach((courtId, slots) -> {
                    var courtDetail = courtMap.get(courtId);
                    if (courtDetail == null) return;

                    List<CourtAvailabilityResponse.BookingBlock> blocks = mergeConsecutiveSlots(slots);
                    courtDetail.setBookings(blocks);
                });

        return CourtAvailabilityResponse.builder()
                .bookingCourts(new ArrayList<>(courtMap.values()))
                .build();
    }

    private List<CourtAvailabilityResponse.BookingBlock> mergeConsecutiveSlots(
            List<CourtRepository.IBookingSlotProjection> slots) {

        if (slots.isEmpty()) return List.of();

        List<CourtAvailabilityResponse.BookingBlock> result = new ArrayList<>();
        slots.sort(Comparator.comparing(CourtRepository.IBookingSlotProjection::getSlotId));

        CourtRepository.IBookingSlotProjection current = slots.get(0);
        LocalTime blockStart = current.getStartTime();
        Long bookingId = current.getBookingId();
        Double totalPrice = current.getTotalPrice();

        for (int i = 1; i < slots.size(); i++) {
            CourtRepository.IBookingSlotProjection next = slots.get(i);

            // Nếu cùng booking và liền nhau (slotId tăng 1)
            if (next.getBookingId().equals(current.getBookingId()) &&
                    next.getSlotId().equals(current.getSlotId() + 1)) {
                current = next;
            } else {
                // Kết thúc block
                result.add(CourtAvailabilityResponse.BookingBlock.builder()
                        .id(bookingId)
                        .bookingDate(slots.get(0).getBookingDate())
                        .startTime(blockStart)
                        .endTime(current.getEndTime())
                        .totalPrice(totalPrice)
                        .build());

                blockStart = next.getStartTime();
                bookingId = next.getBookingId();
                totalPrice = next.getTotalPrice();
                current = next;
            }
        }

        // Thêm block cuối
        result.add(CourtAvailabilityResponse.BookingBlock.builder()
                .id(bookingId)
                .bookingDate(slots.get(0).getBookingDate())
                .startTime(blockStart)
                .endTime(current.getEndTime())
                .totalPrice(totalPrice)
                .build());

        return result;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}