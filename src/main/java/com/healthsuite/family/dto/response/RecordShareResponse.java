package com.healthsuite.family.dto.response;

import com.healthsuite.family.entity.FamilyRecordShare;

import java.util.List;

public record RecordShareResponse(
        Long granteeUserId,
        String scope,
        List<Long> visitIds
) {
    public static RecordShareResponse from(FamilyRecordShare share) {
        return new RecordShareResponse(
                share.getGranteeUserId(),
                share.getScope().name(),
                List.copyOf(share.getVisitIds())
        );
    }
}
