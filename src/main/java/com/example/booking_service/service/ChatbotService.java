package com.example.booking_service.service;

import com.example.booking_service.dto.request.ChatbotRequest;
import com.example.booking_service.dto.response.ChatbotResponse;
import com.example.booking_service.entity.Booking;
import com.example.booking_service.entity.CourtGroup;
import com.example.booking_service.repository.BookingRepository;
import com.example.booking_service.repository.CourtGroupRepository;
import com.example.booking_service.repository.CourtPriceRepository;
import com.example.booking_service.repository.TimeSlotRepository;
import com.example.booking_service.service.gemini.GeminiClient;
import com.example.booking_service.service.gemini.GeminiResult;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ChatbotService {

    private final GeminiClient geminiClient;
    private final CourtGroupRepository courtGroupRepository;
    private final CourtPriceRepository courtPriceRepository;
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;

    public ChatbotService(GeminiClient geminiClient,
                          CourtGroupRepository courtGroupRepository,
                          CourtPriceRepository courtPriceRepository,
                          BookingRepository bookingRepository,
                          TimeSlotRepository timeSlotRepository) {
        this.geminiClient = geminiClient;
        this.courtGroupRepository = courtGroupRepository;
        this.courtPriceRepository = courtPriceRepository;
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public ChatbotResponse handleRequest(ChatbotRequest req) {
        GeminiResult nlu = geminiClient.detectIntent(req);

        String intent = nlu.getIntent();
        if (intent == null) {
            return simpleAnswer("Xin lỗi, mình chưa hiểu câu hỏi của bạn. Bạn có thể nói rõ hơn được không?",
                    "UNKNOWN", List.of());
        }

        switch (intent) {
            case "CHECK_OPENING_HOURS":
                return handleOpeningHours(nlu, req);
            case "CHECK_PRICE":
                return handlePriceQuery(nlu, req);
            case "CHECK_BOOKING_STATUS":
                return handleBookingStatus(nlu, req);
            case "HOW_TO_BOOK":
                return handleHowToBook();
            case "HOW_TO_PAY":
                return handleHowToPay();
            default:
                return fallbackSmallTalk(req);
        }
    }

    private ChatbotResponse handleOpeningHours(GeminiResult nlu, ChatbotRequest req) {
        String message = req.getMessage() == null ? "" : req.getMessage();

        // Ưu tiên dùng entity từ NLU nếu sau này Gemini trả về, còn hiện tại sẽ là null.
        String keyword = nlu.getEntity("court_group_name");
        if (keyword == null || keyword.isBlank()) {
            keyword = extractCourtNameFromMessage(message);
        }

        if (keyword == null || keyword.isBlank()) {
            return simpleAnswer(
                    "Mình chưa nhận diện được tên sân, bạn có thể cung cấp tên sân rõ hơn được không?",
                    "CHECK_OPENING_HOURS",
                    List.of()
            );
        }

        String finalKeyword = keyword.toLowerCase().trim();

        // lấy tất cả sân và filter theo tên chứa keyword (không phân biệt hoa/thường).
        Optional<CourtGroup> cg = courtGroupRepository.findAll().stream()
                .filter(c -> c.getName() != null
                        && c.getName().toLowerCase().contains(finalKeyword)
                        && (c.getIsDeleted() == null || !c.getIsDeleted()))
                .findFirst();

        if (cg.isEmpty()) {
            return simpleAnswer(
                    "Mình chưa tìm thấy sân phù hợp, bạn có thể cung cấp tên sân chi tiết hơn được không?",
                    "CHECK_OPENING_HOURS",
                    List.of()
            );
        }

        CourtGroup group = cg.get();

        String msg = String.format(
                "Sân %s tại %s mở cửa từ %s đến %s.",
                group.getName(),
                group.getAddress(),
                group.getOpenTime(),
                group.getCloseTime()
        );

        return simpleAnswer(
                msg,
                "CHECK_OPENING_HOURS",
                List.of("court_groups.id=" + group.getId())
        );
    }

    private ChatbotResponse handlePriceQuery(GeminiResult nlu, ChatbotRequest req) {
        String message = req.getMessage() == null ? "" : req.getMessage();

        // Extract court name from NLU or message
        String keyword = nlu.getEntity("court_group_name");
        if (keyword == null || keyword.isBlank()) {
            keyword = extractCourtNameFromMessage(message);
        }

        if (keyword == null || keyword.isBlank()) {
            return simpleAnswer(
                    "Mình chưa nhận diện được tên sân, bạn có thể cung cấp tên sân rõ hơn được không?",
                    "CHECK_PRICE",
                    List.of()
            );
        }

        String finalKeyword = keyword.toLowerCase().trim();

        // Find court group by name
        Optional<CourtGroup> cg = courtGroupRepository.findAll().stream()
                .filter(c -> c.getName() != null
                        && c.getName().toLowerCase().contains(finalKeyword)
                        && (c.getIsDeleted() == null || !c.getIsDeleted()))
                .findFirst();

        if (cg.isEmpty()) {
            return simpleAnswer(
                    "Mình chưa tìm thấy sân phù hợp, bạn có thể cung cấp tên sân chi tiết hơn được không?",
                    "CHECK_PRICE",
                    List.of()
            );
        }

        CourtGroup group = cg.get();
        
        // Get all prices for this court group
        List<com.example.booking_service.entity.CourtPrice> prices = 
                courtPriceRepository.findByCourtGroupId(group.getId());
        
        if (prices.isEmpty()) {
            return simpleAnswer(
                    String.format("Sân %s hiện chưa có thông tin giá. Vui lòng liên hệ trực tiếp với chủ sân nhé.", 
                            group.getName()),
                    "CHECK_PRICE",
                    List.of("court_groups.id=" + group.getId())
            );
        }

        // Build price message
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("Giá của sân %s tùy thuộc vào khung giờ và ngày:\n\n", group.getName()));

        // Group prices by day type
        java.util.Map<String, java.util.List<com.example.booking_service.entity.CourtPrice>> pricesByDayType = 
                prices.stream().collect(java.util.stream.Collectors.groupingBy(
                        p -> p.getDayType() != null ? p.getDayType() : "WEEKDAY"
                ));

        // Process WEEKDAY prices
        if (pricesByDayType.containsKey("WEEKDAY")) {
            msg.append("📅 Ngày trong tuần (T2-T6):\n");
            appendPricesForDayType(msg, pricesByDayType.get("WEEKDAY"));
        }

        // Process WEEKEND prices
        if (pricesByDayType.containsKey("WEEKEND")) {
            msg.append("\n📅 Cuối tuần (T7-CN):\n");
            appendPricesForDayType(msg, pricesByDayType.get("WEEKEND"));
        }

        msg.append("\n💡 Lưu ý: Giá đã bao gồm theo giờ chơi.");

        return simpleAnswer(
                msg.toString(),
                "CHECK_PRICE",
                List.of("court_groups.id=" + group.getId())
        );
    }

    private void appendPricesForDayType(StringBuilder msg, 
            java.util.List<com.example.booking_service.entity.CourtPrice> prices) {
        // Sort by time slot ID
        prices.sort(java.util.Comparator.comparing(com.example.booking_service.entity.CourtPrice::getTimeSlotId));

        for (com.example.booking_service.entity.CourtPrice price : prices) {
            // Get time slot info
            com.example.booking_service.entity.TimeSlot timeSlot = 
                    timeSlotRepository.findById(price.getTimeSlotId()).orElse(null);
            
            if (timeSlot != null) {
                // Price is for 30 minutes, so multiply by 2 for hourly rate
                double hourlyPrice = (price.getPrice() != null ? price.getPrice() : 0) * 2;
                
                msg.append(String.format("   ⏰ %s - %s: %,.0f đ/giờ\n",
                        timeSlot.getStartTime(),
                        timeSlot.getEndTime(),
                        hourlyPrice
                ));
            }
        }
    }

    private ChatbotResponse handleBookingStatus(GeminiResult nlu, ChatbotRequest req) {
        // Ưu tiên dùng booking_id từ Gemini entities
        String bookingIdStr = nlu.getEntity("booking_id");
        Long bookingId = null;
        
        if (bookingIdStr != null && !bookingIdStr.isBlank()) {
            try {
                bookingId = Long.parseLong(bookingIdStr.trim());
            } catch (NumberFormatException e) {
                // If parsing fails, try to extract from message
            }
        }
        
        // Fallback: extract from message if Gemini didn't provide it
        if (bookingId == null) {
            String message = req.getMessage() == null ? "" : req.getMessage();
            bookingId = extractBookingId(message);
        }
        
        Long userId = req.getUserContext() != null ? req.getUserContext().getUserId() : null;

        if (bookingId == null) {
            return simpleAnswer(
                    "Bạn vui lòng cung cấp mã đặt sân (ID booking), ví dụ: \"kiểm tra booking 91\" nhé.",
                    "CHECK_BOOKING_STATUS",
                    List.of()
            );
        }

        Optional<Booking> bookingOpt = (userId != null)
                ? bookingRepository.findByIdAndUserId(bookingId, userId)
                : bookingRepository.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            return simpleAnswer(
                    "Mình không tìm thấy thông tin đặt sân với mã " + bookingId + ". " +
                            "Bạn kiểm tra lại mã hoặc đảm bảo bạn đang đăng nhập đúng tài khoản nhé.",
                    "CHECK_BOOKING_STATUS",
                    List.of()
            );
        }

        Booking b = bookingOpt.get();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String bookingDate = b.getBookingDate() != null ? b.getBookingDate().format(dateFormatter) : "";

        String rawStatus = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
        String statusText;
        switch (rawStatus) {
            case "PENDING":
                statusText = "Đang chờ thanh toán/xác nhận";
                break;
            case "CONFIRMED":
                statusText = "Đã xác nhận";
                break;
            case "CANCELLED":
                statusText = "Đã hủy";
                break;
            case "COMPLETED":
                statusText = "Đã hoàn thành";
                break;
            default:
                statusText = rawStatus.isEmpty() ? "Không rõ" : rawStatus;
        }

        String paymentText = (b.getPaymentProof() != null && !b.getPaymentProof().isBlank())
                ? "Hệ thống đã ghi nhận bạn đã gửi minh chứng thanh toán."
                : "Hiện chưa thấy minh chứng thanh toán trên hệ thống.";

        String msg = String.format(
                "Booking #%d ngày %s từ %s đến %s hiện có trạng thái: %s. %s",
                b.getId(),
                bookingDate,
                b.getStartTime(),
                b.getEndTime(),
                statusText,
                paymentText
        );

        return simpleAnswer(
                msg,
                "CHECK_BOOKING_STATUS",
                List.of("bookings.id=" + b.getId())
        );
    }

    private ChatbotResponse handleHowToBook() {
        String msg = "Để đặt sân, bạn hãy:\n" +
                "1) Chọn sân mong muốn trong danh sách.\n" +
                "2) Chọn ngày và khung giờ còn trống.\n" +
                "3) Xác nhận thông tin đặt sân.\n" +
                "4) Thực hiện thanh toán theo hướng dẫn (chuyển khoản/QR) và tải minh chứng nếu cần.\n" +
                "Nếu bạn muốn đặt lịch cố định hàng tuần, hãy liên hệ với chủ sân.";
        return simpleAnswer(msg, "HOW_TO_BOOK", List.of());
    }

    private ChatbotResponse handleHowToPay() {
        String msg = "Hiện tại hệ thống hỗ trợ thanh toán qua chuyển khoản ngân hàng/QR do chủ sân cung cấp.\n" +
                "Sau khi thanh toán, bạn vui lòng tải lên ảnh minh chứng (hóa đơn/biên lai) tại màn hình chi tiết booking " +
                "để chủ sân xác nhận nhanh chóng nhé.";
        return simpleAnswer(msg, "HOW_TO_PAY", List.of());
    }

    private ChatbotResponse fallbackSmallTalk(ChatbotRequest req) {
        String msg = "Mình là chatbot hỗ trợ đặt sân. Bạn có thể hỏi mình về giờ mở cửa, giá sân, " +
                "trạng thái thanh toán, hoặc cách đặt sân/cố định hàng tuần nhé.";
        return simpleAnswer(msg, "SMALL_TALK", List.of());
    }

    private ChatbotResponse simpleAnswer(String text, String intent, List<String> sources) {
        ChatbotResponse resp = new ChatbotResponse();
        resp.setAnswer(text);

        ChatbotResponse.Metadata m = new ChatbotResponse.Metadata();
        m.setIntent(intent);
        m.setSources(sources);

        resp.setMetadata(m);
        return resp;
    }

    private Long extractBookingId(String message) {
        String[] tokens = message.split("\\D+");
        for (String token : tokens) {
            if (!token.isBlank()) {
                try {
                    return Long.parseLong(token);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Heuristic đơn giản để tách tên sân từ câu hỏi tiếng Việt, ví dụ:
     * "giờ mở cửa của sân thành công" -> "thành công"
     * "giờ mở cửa sân Thành Công ở Giải Phóng" -> "thành công ở giải phóng"
     */
    private String extractCourtNameFromMessage(String message) {
        String lower = message.toLowerCase();

        int idx = lower.indexOf("sân");
        if (idx >= 0) {
            String after = lower.substring(idx + "sân".length()).trim();
            // bỏ bớt các từ dư ở đầu
            after = after.replaceFirst("^(cầu lông|bóng đá)\\s+", "");
            after = after.replaceFirst("^(ở|tại|quận|huyện)\\s+", "");

            // cắt bỏ phần câu hỏi phía sau như "mở cửa", "giờ mở cửa", "vào lúc nào"...
            after = after.replaceAll("(mở cửa|giờ mở cửa|vào lúc nào|lúc nào).*", "").trim();

            // bỏ dấu ? . , ở cuối
            after = after.replaceAll("[\\?\\.!,]+$", "").trim();
            return after;
        }

        // nếu không có chữ "sân", fallback: bỏ bớt cụm "giờ mở cửa", "mở cửa"
        String cleaned = lower
                .replace("giờ mở cửa", "")
                .replace("mở cửa", "")
                .replace("mấy giờ", "")
                .replace("mấy h", "")
                .replace("cho mình hỏi", "")
                .trim();

        return cleaned.isEmpty() ? null : cleaned;
    }
}


