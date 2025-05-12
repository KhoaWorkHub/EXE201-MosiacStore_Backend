package com.mosiacstore.mosiac.infrastructure.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface StorageService {
    String uploadFile(MultipartFile file, String folder);
    void deleteFile(String fileUrl);
    InputStream getFile(String objectName);
}