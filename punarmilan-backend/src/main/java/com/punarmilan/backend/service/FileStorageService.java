package com.punarmilan.backend.service;

import com.punarmilan.backend.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        try {
            // Get absolute path and normalize
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

            // Create directory if it doesn't exist
            Files.createDirectories(this.fileStorageLocation);

            log.info("✅ File upload directory initialized at: {}",
                    this.fileStorageLocation.toString());
            log.info("✅ Directory exists: {}", Files.exists(this.fileStorageLocation));
            log.info("✅ Directory writable: {}", Files.isWritable(this.fileStorageLocation));

        } catch (Exception ex) {
            log.error("❌ Could not create upload directory: {}", ex.getMessage());
            throw new FileStorageException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new FileStorageException("File is empty");
            }

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new FileStorageException("File name is empty");
            }

            log.info("📁 Uploading file: {} ({} bytes, type: {})",
                    originalFileName, file.getSize(), file.getContentType());

            // Get file extension
            String fileExtension = getFileExtension(originalFileName);

            // Generate unique filename
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Resolve target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            log.info("📁 Target location: {}", targetLocation.toString());

            // Copy file to target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Verify file was saved
            long fileSize = Files.size(targetLocation);
            log.info("✅ File saved successfully: {} ({} bytes)", uniqueFileName, fileSize);

            // Build URL
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .build()
                    .toUriString();

            String fullUrl = baseUrl + "/uploads/" + uniqueFileName;
            log.info("🔗 Generated URL: {}", fullUrl);

            return fullUrl;

        } catch (IOException ex) {
            log.error("❌ Error storing file: {}", ex.getMessage());
            throw new FileStorageException("Could not store file: " + ex.getMessage(), ex);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return ".jpg";
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            String ext = fileName.substring(lastDotIndex).toLowerCase();
            // Ensure valid image and document extensions
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") ||
                    ext.equals(".gif") || ext.equals(".bmp") || ext.equals(".pdf")) {
                return ext;
            }
        }
        return ".jpg";
    }

    // Optional helper method to delete files
    public boolean deleteFile(String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                return false;
            }

            // Extract filename from URL
            String simpleFileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            Path filePath = this.fileStorageLocation.resolve(simpleFileName);

            if (Files.exists(filePath)) {
                boolean deleted = Files.deleteIfExists(filePath);
                log.info("🗑️ File {}: {}", simpleFileName, deleted ? "deleted" : "not found");
                return deleted;
            }
            return false;
        } catch (Exception ex) {
            log.error("Error deleting file: {}", ex.getMessage());
            return false;
        }
    }
}