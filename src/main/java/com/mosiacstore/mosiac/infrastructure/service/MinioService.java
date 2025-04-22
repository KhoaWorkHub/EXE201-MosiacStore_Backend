package com.mosiacstore.mosiac.infrastructure.service;

import com.mosiacstore.mosiac.infrastructure.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * Upload file to MinIO server
     * @param file The file to upload
     * @param folder Folder path (example: "products")
     * @return The URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Check if bucket exists
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .build());
            }

            // Generate unique filename
            String filename = generateUniqueFilename(file.getOriginalFilename());
            String objectName = folder + "/" + filename;

            // Upload file
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // Construct URL (depends on your MinIO configuration)
            return constructFileUrl(objectName);

        } catch (Exception e) {
            log.error("Error uploading file to MinIO", e);
            throw new RuntimeException("Could not upload file", e);
        }
    }

    /**
     * Delete file from MinIO server
     * @param fileUrl The URL of the file to delete
     */
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectNameFromUrl(fileUrl);

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());

        } catch (Exception e) {
            log.error("Error deleting file from MinIO", e);
            throw new RuntimeException("Could not delete file", e);
        }
    }

    /**
     * Get file from MinIO server
     * @param objectName The object name of the file
     * @return InputStream of the file
     */
    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("Error getting file from MinIO", e);
            throw new RuntimeException("Could not get file", e);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String constructFileUrl(String objectName) {
        // This URL format depends on your MinIO configuration
        String protocol = minioConfig.isSecure() ? "https://" : "http://";
        return protocol + minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
    }

    private String extractObjectNameFromUrl(String fileUrl) {
        // Extract object name from URL
        // This logic depends on your URL format
        String baseUrl = minioConfig.isSecure() ? "https://" : "http://";
        baseUrl += minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/";

        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        throw new IllegalArgumentException("Invalid file URL format");
    }
}