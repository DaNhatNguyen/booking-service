package com.example.booking_service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class to build full URLs for uploaded files
 */
@Component
public class FileUrlBuilder {
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    /**
     * Build full URL for a single filename
     * @param filename The filename (e.g., "uuid.jpg")
     * @return Full URL (e.g., "http://localhost:8080/api/files/court-images/uuid.jpg")
     */
    public String buildImageUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        
        String baseUrl = getBaseUrl();
        String path = contextPath != null && !contextPath.isEmpty() ? contextPath : "";
        return baseUrl + path + "/files/court-images/" + filename;
    }
    
    /**
     * Build full URLs for comma-separated filenames
     * @param filenames Comma-separated filenames (e.g., "uuid1.jpg,uuid2.jpg")
     * @return Comma-separated full URLs
     */
    public String buildImageUrls(String filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return null;
        }
        
        String baseUrl = getBaseUrl();
        String path = contextPath != null && !contextPath.isEmpty() ? contextPath : "";
        String[] files = filenames.split(",");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < files.length; i++) {
            String filename = files[i].trim();
            if (!filename.isEmpty()) {
                if (i > 0) {
                    result.append(",");
                }
                result.append(baseUrl).append(path).append("/files/court-images/").append(filename);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get base URL from current request
     * @return Base URL (e.g., "http://localhost:8080")
     */
    private String getBaseUrl() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                
                StringBuilder url = new StringBuilder();
                url.append(scheme).append("://").append(serverName);
                
                // Only append port if it's not the default port
                if ((scheme.equals("http") && serverPort != 80) || 
                    (scheme.equals("https") && serverPort != 443)) {
                    url.append(":").append(serverPort);
                }
                
                return url.toString();
            }
        } catch (Exception e) {
            // Fallback to default
        }
        
        // Fallback: use default localhost:8080
        return "http://localhost:8080";
    }
}

