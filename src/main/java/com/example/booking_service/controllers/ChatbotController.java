package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ChatbotRequest;
import com.example.booking_service.dto.response.ChatbotResponse;
import com.example.booking_service.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatbotResponse> query(@RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.handleRequest(request);
        return ResponseEntity.ok(response);
    }
}


