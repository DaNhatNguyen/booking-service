package com.example.booking_service.controllers;

import com.example.booking_service.dto.request.ApiResponse;
import com.example.booking_service.service.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {

    FileStorageService fileStorageService;

    @GetMapping("/court-images/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.getFileStorageLocation().resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                
                // Determine content type based on file extension
                if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Upload single image
     * Returns full URL instead of just filename
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeFile(file);
        String fileUrl = fileStorageService.getFileUrl(filename);
        
        Map<String, String> result = new HashMap<>();
        result.put("filename", filename);
        result.put("url", fileUrl);
        
        return ApiResponse.<Map<String, String>>builder()
                .message("File uploaded successfully")
                .result(result)
                .build();
    }
    
    /**
     * Upload multiple images
     * Returns list of full URLs
     */
    @PostMapping("/upload-multiple")
    public ApiResponse<List<Map<String, String>>> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        List<String> filenames = fileStorageService.storeFiles(files);
        List<Map<String, String>> result = new ArrayList<>();
        
        for (String filename : filenames) {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("filename", filename);
            fileInfo.put("url", fileStorageService.getFileUrl(filename));
            result.add(fileInfo);
        }
        
        return ApiResponse.<List<Map<String, String>>>builder()
                .message("Files uploaded successfully")
                .result(result)
                .build();
    }
}




