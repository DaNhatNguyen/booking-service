package com.example.booking_service.service.gemini;

import com.example.booking_service.configuration.GeminiProperties;
import com.example.booking_service.dto.request.ChatbotRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * HTTP client for calling Google Gemini API (Google AI Studio).
 * Uses structured prompts to extract intent and entities from user messages.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
    }

    public GeminiResult detectIntent(ChatbotRequest request) {
        // If API key is not configured, fall back to a naive rule-based intent
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("Gemini API key is not configured. Falling back to simple intent detection.");
            return simpleDetectIntent(request.getMessage());
        }

        try {
            String userMessage = request.getMessage() == null ? "" : request.getMessage();
            String prompt = buildPrompt(userMessage, request.getHistory());

            String url = buildApiUrl();
            Map<String, Object> requestBody = buildRequestBody(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling Gemini API: {}", url.replace(properties.getApiKey(), "***"));
            log.debug("Request body: {}", requestBody);
            
            String responseJson = restTemplate.postForObject(url, entity, String.class);

            log.debug("Gemini API response: {}", responseJson);

            // Check for error in response
            if (responseJson != null && responseJson.contains("\"error\"")) {
                log.error("Gemini API returned error: {}", responseJson);
                throw new RuntimeException("Gemini API error: " + responseJson);
            }

            GeminiResult result = parseGeminiResponse(responseJson);
            result.setRawResponse(responseJson);

            // If parsing failed, fallback to simple detection
            if (result.getIntent() == null || result.getIntent().isBlank()) {
                log.warn("Failed to parse intent from Gemini response, using fallback. Response: {}", responseJson);
                GeminiResult fallback = simpleDetectIntent(userMessage);
                result.setIntent(fallback.getIntent());
                if (result.getEntities().isEmpty()) {
                    result.setEntities(fallback.getEntities());
                }
            } else {
                log.info("Successfully parsed intent: {} with entities: {}", result.getIntent(), result.getEntities());
            }

            return result;
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            log.error("HTTP error calling Gemini API (status: {}): {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return simpleDetectIntent(request.getMessage());
        } catch (org.springframework.web.client.HttpServerErrorException ex) {
            log.error("Server error calling Gemini API (status: {}): {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return simpleDetectIntent(request.getMessage());
        } catch (Exception ex) {
            log.error("Error calling Gemini API, fallback to simple intent detection", ex);
            log.error("Exception details: {}", ex.getMessage());
            if (ex.getCause() != null) {
                log.error("Caused by: {}", ex.getCause().getMessage());
            }
            return simpleDetectIntent(request.getMessage());
        }
    }

    private String buildPrompt(String userMessage, List<ChatbotRequest.Message> history) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là chatbot hỗ trợ khách hàng cho hệ thống đặt sân thể thao. ");
        prompt.append("Nhiệm vụ của bạn là phân tích câu hỏi của người dùng và trả về JSON với format sau:\n\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"<INTENT>\",\n");
        prompt.append("  \"entities\": {\n");
        prompt.append("    \"court_group_name\": \"<tên sân nếu có>\",\n");
        prompt.append("    \"booking_id\": \"<mã booking nếu có>\",\n");
        prompt.append("    \"district\": \"<quận/huyện nếu có>\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("Các intent có thể là:\n");
        prompt.append("- CHECK_OPENING_HOURS: Khi người dùng hỏi về giờ mở/đóng cửa của sân\n");
        prompt.append("- CHECK_PRICE: Khi người dùng hỏi về giá sân\n");
        prompt.append("- CHECK_BOOKING_STATUS: Khi người dùng hỏi về trạng thái booking/thanh toán\n");
        prompt.append("- HOW_TO_BOOK: Khi người dùng hỏi cách đặt sân\n");
        prompt.append("- HOW_TO_PAY: Khi người dùng hỏi cách thanh toán\n");
        prompt.append("- SMALL_TALK: Các câu hỏi chào hỏi, trò chuyện thông thường\n\n");
        
        prompt.append("Lưu ý:\n");
        prompt.append("- Chỉ trả về JSON, không thêm text nào khác\n");
        prompt.append("- Nếu không tìm thấy entity, để giá trị là null hoặc bỏ qua field đó\n");
        prompt.append("- Tên sân có thể xuất hiện ở nhiều vị trí trong câu, hãy trích xuất chính xác\n\n");
        
        if (history != null && !history.isEmpty()) {
            prompt.append("Lịch sử hội thoại:\n");
            for (ChatbotRequest.Message msg : history) {
                prompt.append(String.format("%s: %s\n", msg.getRole(), msg.getContent()));
            }
            prompt.append("\n");
        }
        
        prompt.append("Câu hỏi của người dùng: ").append(userMessage).append("\n\n");
        prompt.append("Hãy phân tích và trả về JSON:");

        return prompt.toString();
    }

    private String buildApiUrl() {
        String model = properties.getModel() != null && !properties.getModel().isBlank() 
                ? properties.getModel() 
                : "gemini-2.5-flash";
        
        // If baseUrl is provided, use it; otherwise construct default Google AI Studio URL
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            return properties.getBaseUrl() + "?key=" + properties.getApiKey();
        }
        
        // Use v1beta API for Gemini models
        return String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model,
                properties.getApiKey()
        );
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> body = new HashMap<>();
        
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        
        body.put("contents", contents);
        
        // Optional: Add generation config for better JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1); // Lower temperature for more deterministic output
        generationConfig.put("topK", 1);
        generationConfig.put("topP", 0.8);
        body.put("generationConfig", generationConfig);
        
        return body;
    }

    private GeminiResult parseGeminiResponse(String responseJson) {
        GeminiResult result = new GeminiResult();
        
        if (responseJson == null || responseJson.isBlank()) {
            log.warn("Empty response from Gemini API");
            return result;
        }
        
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            
            // Check for error first
            if (root.has("error")) {
                JsonNode error = root.path("error");
                String errorMessage = error.path("message").asText("Unknown error");
                log.error("Gemini API error: {}", errorMessage);
                return result;
            }
            
            // Extract text from response
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                
                // Check for finishReason (might be blocked or filtered)
                if (firstCandidate.has("finishReason")) {
                    String finishReason = firstCandidate.path("finishReason").asText("");
                    if (!"STOP".equals(finishReason)) {
                        log.warn("Gemini response finishReason: {}", finishReason);
                    }
                }
                
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    JsonNode textPart = parts.get(0);
                    String text = textPart.path("text").asText("");
                    
                    log.debug("Extracted text from Gemini: {}", text);
                    
                    // Try to extract JSON from the text response
                    String jsonText = extractJsonFromText(text);
                    
                    if (jsonText != null && !jsonText.isBlank()) {
                        log.debug("Extracted JSON from text: {}", jsonText);
                        JsonNode intentNode = objectMapper.readTree(jsonText);
                        
                        // Extract intent
                        String intent = intentNode.path("intent").asText(null);
                        result.setIntent(intent);
                        
                        // Extract entities
                        JsonNode entitiesNode = intentNode.path("entities");
                        if (entitiesNode.isObject()) {
                            Map<String, String> entities = new HashMap<>();
                            entitiesNode.fields().forEachRemaining(entry -> {
                                String value = entry.getValue().asText(null);
                                if (value != null && !value.equals("null") && !value.isBlank()) {
                                    entities.put(entry.getKey(), value);
                                }
                            });
                            result.setEntities(entities);
                        }
                    } else {
                        log.warn("Could not extract JSON from Gemini response text: {}", text);
                    }
                } else {
                    log.warn("No parts found in Gemini response content");
                }
            } else {
                log.warn("No candidates found in Gemini response");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Error parsing Gemini response JSON: {}", responseJson, ex);
        } catch (Exception ex) {
            log.error("Unexpected error parsing Gemini response", ex);
        }
        
        return result;
    }

    private String extractJsonFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        // Try to find JSON object in the text
        int startIdx = text.indexOf("{");
        int endIdx = text.lastIndexOf("}");
        
        if (startIdx >= 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + 1);
        }
        
        // If no JSON found, return null
        return null;
    }

    private GeminiResult simpleDetectIntent(String message) {
        String lower = message == null ? "" : message.toLowerCase();
        GeminiResult result = new GeminiResult();

        if (lower.contains("giờ mở") || lower.contains("mấy giờ mở") || lower.contains("open")) {
            result.setIntent("CHECK_OPENING_HOURS");
        } else if (lower.contains("giá") || lower.contains("bao nhiêu") || lower.contains("price")) {
            result.setIntent("CHECK_PRICE");
        } else if (lower.contains("trạng thái thanh toán") || lower.contains("booking") || lower.contains("mã đặt")) {
            result.setIntent("CHECK_BOOKING_STATUS");
        } else if (lower.contains("thanh toán") || lower.contains("chuyển khoản")) {
            result.setIntent("HOW_TO_PAY");
        } else if (lower.contains("đặt sân cố định") || lower.contains("lịch cố định") || lower.contains("đặt sân")) {
            result.setIntent("HOW_TO_BOOK");
        } else {
            result.setIntent("SMALL_TALK");
        }

        // naive extraction: court name as full message for now
        result.getEntities().put("raw_message", message);
        return result;
    }
}


