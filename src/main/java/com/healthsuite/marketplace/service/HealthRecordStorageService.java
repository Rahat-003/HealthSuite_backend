package com.healthsuite.marketplace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * Local file store for patient health records (prescription copies).
 * Layout: {root}/{patientUserId}/{yyyyMMdd}/{uuid}-{sanitized-name}
 */
@Service
@Slf4j
public class HealthRecordStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    private final Path rootDir;

    public HealthRecordStorageService(
            @Value("${app.storage.healthrecord.path:#{systemProperties['user.home']}/healthsuite/healthrecord}") String rootPath
    ) throws IOException {
        this.rootDir = Paths.get(rootPath).toAbsolutePath().normalize();
        Files.createDirectories(rootDir);
        log.info("Health-record storage initialized at: {}", rootDir);
    }

    public record StoredFile(String fileName, String filePath, String contentType, long sizeBytes) {}

    public StoredFile store(Long patientUserId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new com.healthsuite.common.exception.BadRequestException(
                "Only JPEG/PNG/WebP images or PDF files are allowed for prescriptions.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new com.healthsuite.common.exception.BadRequestException("Prescription file must be 10 MB or smaller.");
        }

        Path dir = rootDir
            .resolve(String.valueOf(patientUserId))
            .resolve(LocalDate.now().format(DATE_FMT));
        try {
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            Path dest = dir.resolve(fileName);
            file.transferTo(dest);
            log.debug("Stored health-record file: {}", dest);
            return new StoredFile(fileName, dest.toString(), contentType, file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store prescription file: " + e.getMessage(), e);
        }
    }

    private String sanitize(String name) {
        if (name == null) return "upload";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
