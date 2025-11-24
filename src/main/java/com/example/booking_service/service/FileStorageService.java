package com.example.booking_service.service;

import com.example.booking_service.util.FileUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;
    
    @Autowired
    private FileUrlBuilder fileUrlBuilder;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return fileName;
        } catch (IOException ex) {
            log.error("Could not store file. Error: ", ex);
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public List<String> storeFiles(List<MultipartFile> files) {
        List<String> fileNames = new ArrayList<>();
        
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = storeFile(file);
                    fileNames.add(fileName);
                }
            }
        }
        
        return fileNames;
    }
    
    /**
     * Get full URL for a filename
     * @param filename The filename
     * @return Full URL to access the file
     */
    public String getFileUrl(String filename) {
        return fileUrlBuilder.buildImageUrl(filename);
    }
    
    /**
     * Get full URLs for comma-separated filenames
     * @param filenames Comma-separated filenames
     * @return Comma-separated full URLs
     */
    public String getFileUrls(String filenames) {
        return fileUrlBuilder.buildImageUrls(filenames);
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.error("Could not delete file {}. Error: ", fileName, ex);
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}




