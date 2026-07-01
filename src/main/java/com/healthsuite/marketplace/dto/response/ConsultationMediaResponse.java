package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.ConsultationMedia;
import com.healthsuite.marketplace.enums.ConsultationMediaType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultationMediaResponse(
    Long id,
    Long consultationId,
    UUID userUuid,
    String fileName,
    String filePath,
    ConsultationMediaType mediaType,
    LocalDateTime createdAt
) {
    public static ConsultationMediaResponse from(ConsultationMedia m) {
        return new ConsultationMediaResponse(
            m.getId(), m.getConsultationId(), m.getUserUuid(),
            m.getFileName(), m.getFilePath(), m.getMediaType(), m.getCreatedAt()
        );
    }
}
