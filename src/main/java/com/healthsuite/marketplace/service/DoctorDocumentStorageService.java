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
import java.util.UUID;

/**
 * Private storage for doctor credential documents (certificates, headshots).
 * Not exposed via /files/** — admin access goes through an authorized download endpoint.
 */
@Service
@Slf4j
public class DoctorDocumentStorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path baseDir;

    public DoctorDocumentStorageService(
            @Value("${app.storage.doctor.path:#{systemProperties['user.home']}/HealthSuite/doctor_docs}") String basePath
    ) throws IOException {
        this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        log.info("Doctor document storage initialized at: {}", baseDir);
    }

    public record StoredFile(String fileName, String filePath) {}

    public StoredFile store(MultipartFile file) {
        String date = LocalDate.now().format(DATE_FMT);
        Path dir = baseDir.resolve(date);
        try {
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
            Path dest = dir.resolve(fileName);
            file.transferTo(dest);
            log.debug("Stored doctor document: {}", dest);
            return new StoredFile(fileName, dest.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store doctor document: " + e.getMessage(), e);
        }
    }

    private String sanitize(String name) {
        if (name == null) return "upload";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
