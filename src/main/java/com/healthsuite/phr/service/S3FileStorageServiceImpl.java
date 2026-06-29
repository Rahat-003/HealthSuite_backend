package com.healthsuite.phr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageServiceImpl implements FileStorageService {

    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.aws.s3.region}")
    private String region;

    private final S3Client s3Client;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String key = folder + "/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            log.debug("Uploaded file to S3: {}", url);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        log.debug("Deleted file from S3: {}", key);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "upload";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractKeyFromUrl(String url) {
        // https://bucket.s3.region.amazonaws.com/key/path
        int idx = url.indexOf(".amazonaws.com/");
        if (idx == -1) throw new IllegalArgumentException("Not a valid S3 URL: " + url);
        return url.substring(idx + ".amazonaws.com/".length());
    }
}
