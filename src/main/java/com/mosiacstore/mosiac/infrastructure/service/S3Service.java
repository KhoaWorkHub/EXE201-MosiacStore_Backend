package com.mosiacstore.mosiac.infrastructure.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.mosiacstore.mosiac.infrastructure.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3Service implements StorageService {

    private final AmazonS3 s3Client;
    private final S3Config s3Config;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Generate unique filename
            String filename = generateUniqueFilename(file.getOriginalFilename());
            String objectKey = folder + "/" + filename;

            // Prepare metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // Upload file
            s3Client.putObject(
                    s3Config.getBucket(),
                    objectKey,
                    file.getInputStream(),
                    metadata
            );

            // Construct and return URL
            return constructFileUrl(objectKey);
        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Could not upload file", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String objectKey = extractObjectKeyFromUrl(fileUrl);
            s3Client.deleteObject(new DeleteObjectRequest(s3Config.getBucket(), objectKey));
            log.info("Successfully deleted file from S3: {}", objectKey);
        } catch (Exception e) {
            log.error("Error deleting file from S3", e);
            throw new RuntimeException("Could not delete file", e);
        }
    }

    @Override
    public InputStream getFile(String objectName) {
        try {
            S3Object object = s3Client.getObject(s3Config.getBucket(), objectName);
            return object.getObjectContent();
        } catch (Exception e) {
            log.error("Error getting file from S3", e);
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

    private String constructFileUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3Config.getBucket(),
                s3Config.getRegion(),
                objectKey);
    }

    private String extractObjectKeyFromUrl(String fileUrl) {
        // Extract the object key from a S3 URL
        String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/",
                s3Config.getBucket(),
                s3Config.getRegion());

        if (fileUrl.startsWith(baseUrl)) {
            return fileUrl.substring(baseUrl.length());
        }

        throw new IllegalArgumentException("Invalid S3 URL format: " + fileUrl);
    }
}