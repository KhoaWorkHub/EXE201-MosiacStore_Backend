package com.mosiacstore.mosiac.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

@Service
@Slf4j
public class StorageServiceDelegate implements StorageService {

    private final MinioService minioService;
    private final Optional<S3Service> s3Service;

    @Value("${storage.type:minio}")
    private String storageType;

    public StorageServiceDelegate(MinioService minioService,
                                  Optional<S3Service> s3Service) {
        this.minioService = minioService;
        this.s3Service = s3Service;
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        if ("s3".equals(storageType) && s3Service.isPresent()) {
            log.info("Using AWS S3 for file storage");
            return s3Service.get().uploadFile(file, folder);
        } else {
            log.info("Using MinIO for file storage");
            return minioService.uploadFile(file, folder);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if ("s3".equals(storageType) && s3Service.isPresent()) {
            log.info("Using AWS S3 for file deletion");
            s3Service.get().deleteFile(fileUrl);
        } else {
            log.info("Using MinIO for file deletion");
            minioService.deleteFile(fileUrl);
        }
    }

    @Override
    public InputStream getFile(String objectName) {
        if ("s3".equals(storageType) && s3Service.isPresent()) {
            log.info("Using AWS S3 for file retrieval");
            return s3Service.get().getFile(objectName);
        } else {
            log.info("Using MinIO for file retrieval");
            return minioService.getFile(objectName);
        }
    }
}