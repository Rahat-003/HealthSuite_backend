package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.Specialty;

public record SpecialtyResponse(
    Long id,
    String name,
    String nameBn,
    String descEn,
    String descBn,
    String icon
) {
    public static SpecialtyResponse from(Specialty s) {
        return new SpecialtyResponse(s.getId(), s.getName(), s.getNameBn(), s.getDescEn(), s.getDescBn(), s.getIcon());
    }
}
