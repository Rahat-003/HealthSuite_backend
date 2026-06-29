package com.healthsuite.phr.dto.response;

import com.healthsuite.phr.entity.VisitDocument;

import java.time.Instant;

public record VisitDocumentResponse(
        Long id,
        String fileUrl,
        String fileName,
        String fileType,
        Instant uploadedAt
) {
    public static VisitDocumentResponse from(VisitDocument d) {
        return new VisitDocumentResponse(d.getId(), d.getFileUrl(), d.getFileName(),
                d.getFileType(), d.getUploadedAt());
    }
}
