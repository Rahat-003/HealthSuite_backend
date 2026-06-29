package com.healthsuite.scraper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScrapedData {

    private String bmdcNumber;
    private String fullName;
    private String specialty;
    private String chamberAddress;
    private String hospitalName;
    private BigDecimal consultationFee;
    private String availableSlots;
    private String phoneNumber;
    private String profileUrl;
}
