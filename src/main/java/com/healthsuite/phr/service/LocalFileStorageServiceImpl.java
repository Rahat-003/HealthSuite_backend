package com.healthsuite.phr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path baseDir;

    public LocalFileStorageServiceImpl(@Value("${app.storage.local.path}") String basePath) throws IOException {
        this.baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        log.info("Local file storage initialized at: {}", baseDir);
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String filename = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        Path dest = baseDir.resolve(folder).resolve(filename);
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
            String url = "/files/" + folder + "/" + filename;
            log.debug("Stored file locally: {}", dest);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        // fileUrl is like /files/phr/uuid-name.pdf
        String relative = fileUrl.replaceFirst("^/files/", "");
        Path target = baseDir.resolve(relative);
        try {
            Files.deleteIfExists(target);
            log.debug("Deleted local file: {}", target);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", target, e.getMessage());
        }
    }

    private String sanitize(String filename) {
        if (filename == null) return "upload";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
