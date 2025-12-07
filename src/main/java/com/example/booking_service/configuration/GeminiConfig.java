package com.example.booking_service.configuration;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);
    private final GeminiProperties properties;

    public GeminiConfig(GeminiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void logConfiguration() {
        log.info("Gemini configuration loaded - API Key: {}, Model: {}, Base URL: {}", 
                properties.getApiKey() != null && !properties.getApiKey().isBlank() ? "***configured***" : "NOT SET",
                properties.getModel() != null ? properties.getModel() : "NOT SET",
                properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank() ? properties.getBaseUrl() : "will use default");
    }
}













