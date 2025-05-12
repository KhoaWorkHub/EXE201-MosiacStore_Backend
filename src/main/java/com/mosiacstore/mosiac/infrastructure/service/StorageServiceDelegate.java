package com.mosiacstore.mosiac.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceDelegate implements StorageService {

    private final MinioService minioService;

    @Value("${storage.type:minio}")
    private String storageType;

    private final S3Service s3Service;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if ("s3".equals(storageType)) {
            log.info("Using AWS S3 for file storage");
            return s3Service.uploadFile(file, folder);
        } else {
            log.info("Using MinIO for file storage");
            return minioService.uploadFile(file, folder);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if ("s3".equals(storageType)) {
            s3Service.deleteFile(fileUrl);
        } else {
            minioService.deleteFile(fileUrl);
        }
    }

    @Override
    public InputStream getFile(String objectName) {
        if ("s3".equals(storageType)) {
            return s3Service.getFile(objectName);
        } else {
            return minioService.getFile(objectName);
        }
    }
}