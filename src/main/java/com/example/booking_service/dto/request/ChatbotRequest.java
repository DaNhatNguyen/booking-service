package com.example.booking_service.dto.request;

import java.util.List;

public class ChatbotRequest {

    private String message;
    private String conversationId;
    private List<Message> history;
    private UserContext userContext;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<Message> getHistory() {
        return history;
    }

    public void setHistory(List<Message> history) {
        this.history = history;
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }

    public static class Message {
        private String role; // "user" | "assistant"
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class UserContext {
        private Long userId;
        private String preferredDistrict;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getPreferredDistrict() {
            return preferredDistrict;
        }

        public void setPreferredDistrict(String preferredDistrict) {
            this.preferredDistrict = preferredDistrict;
        }
    }
}












