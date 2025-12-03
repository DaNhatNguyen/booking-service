package com.example.booking_service.service.gemini;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple DTO to represent the parsed result from Gemini
 * (intent + extracted entities).
 */
public class GeminiResult {

    private String intent;
    private Map<String, String> entities = new HashMap<>();
    private String rawResponse;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Map<String, String> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getEntity(String key) {
        return entities != null ? entities.get(key) : null;
    }
}









