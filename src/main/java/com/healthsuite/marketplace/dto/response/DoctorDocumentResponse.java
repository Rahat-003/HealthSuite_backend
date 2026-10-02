package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.DoctorDocument;
import com.healthsuite.marketplace.enums.DoctorDocumentType;

import java.time.LocalDateTime;

public record DoctorDocumentResponse(
    Long id,
    DoctorDocumentType docType,
    String fileName,
    LocalDateTime createdAt
) {
    public static DoctorDocumentResponse from(DoctorDocument d) {
        return new DoctorDocumentResponse(d.getId(), d.getDocType(), d.getFileName(), d.getCreatedAt());
    }
}
