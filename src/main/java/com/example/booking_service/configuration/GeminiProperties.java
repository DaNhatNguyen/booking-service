package com.example.booking_service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /**
     * API key for calling Gemini (Google AI Studio / Vertex AI).
     */
    private String apiKey;

    /**
     * Model name, e.g. "gemini-1.5-flash".
     */
    private String model;

    /**
     * Base URL for Gemini HTTP endpoint.
     */
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}


