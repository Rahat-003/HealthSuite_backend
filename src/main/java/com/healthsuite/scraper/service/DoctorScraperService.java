package com.healthsuite.scraper.service;

import com.healthsuite.scraper.config.ScraperConfig;
import com.healthsuite.scraper.dto.DoctorScrapedData;
import com.healthsuite.scraper.entity.Doctor;
import com.healthsuite.scraper.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorScraperService {

    private final ScraperConfig scraperConfig;
    private final DoctorRepository doctorRepository;
    private final List<ScraperStrategy> strategies;

    /**
     * Fires daily at 02:00 AM.
     * Selects the appropriate scraper strategy per URL and upserts results.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void scrapeAndUpsert() {
        List<String> urls = scraperConfig.getTargetUrls().stream()
                .filter(u -> u != null && !u.isBlank())
                .toList();

        if (urls.isEmpty()) {
            log.info("No scraper target URLs configured — skipping");
            return;
        }

        log.info("Doctor scraper started — {} target URLs", urls.size());
        int inserted = 0, updated = 0;

        for (String url : urls) {
            ScraperStrategy strategy = selectStrategy(url);
            List<DoctorScrapedData> scraped;

            try {
                scraped = strategy.scrape(url);
            } catch (Exception e) {
                log.error("Scrape failed for URL {}: {}", url, e.getMessage());
                continue;
            }

            for (DoctorScrapedData data : scraped) {
                try {
                    boolean wasNew = upsert(data);
                    if (wasNew) inserted++; else updated++;
                } catch (Exception e) {
                    log.warn("Failed to upsert doctor '{}': {}", data.getFullName(), e.getMessage());
                }
            }
        }

        log.info("Doctor scraper finished — {} inserted, {} updated", inserted, updated);
    }

    /**
     * Upsert rule from spec:
     * 1. Match on BMDC number (authoritative identifier).
     * 2. Fall back to (full_name + specialty + chamber_address).
     * 3. Update on match, insert on miss.
     *
     * @return true if a new record was inserted, false if an existing one was updated
     */
    public boolean upsert(DoctorScrapedData data) {
        Optional<Doctor> existing = Optional.empty();

        if (data.getBmdcNumber() != null && !data.getBmdcNumber().isBlank()) {
            existing = doctorRepository.findByBmdcNumber(data.getBmdcNumber());
        }

        if (existing.isEmpty() && data.getFullName() != null) {
            existing = doctorRepository.findByFullNameAndSpecialtyAndChamberAddress(
                    data.getFullName(), data.getSpecialty(), data.getChamberAddress());
        }

        Doctor doctor = existing.orElseGet(Doctor::new);
        boolean isNew = doctor.getId() == null;

        mapFields(doctor, data);
        doctor.setLastScrapedAt(Instant.now());
        if (!isNew) {
            doctor.setUpdatedAt(Instant.now());
        }

        doctorRepository.save(doctor);
        return isNew;
    }

    private void mapFields(Doctor doctor, DoctorScrapedData data) {
        if (data.getBmdcNumber() != null) doctor.setBmdcNumber(data.getBmdcNumber());
        if (data.getFullName() != null) doctor.setFullName(data.getFullName());
        if (data.getSpecialty() != null) doctor.setSpecialty(data.getSpecialty());
        if (data.getChamberAddress() != null) doctor.setChamberAddress(data.getChamberAddress());
        if (data.getHospitalName() != null) doctor.setHospitalName(data.getHospitalName());
        if (data.getConsultationFee() != null) doctor.setConsultationFee(data.getConsultationFee());
        if (data.getAvailableSlots() != null) doctor.setAvailableSlots(data.getAvailableSlots());
        if (data.getPhoneNumber() != null) doctor.setPhoneNumber(data.getPhoneNumber());
        if (data.getProfileUrl() != null) doctor.setProfileUrl(data.getProfileUrl());
    }

    private ScraperStrategy selectStrategy(String url) {
        return strategies.stream()
                .filter(s -> s.supports(url) && !(s instanceof JsoupScraperStrategy))
                .findFirst()
                .orElseGet(() -> strategies.stream()
                        .filter(s -> s instanceof JsoupScraperStrategy)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No scraper strategy available")));
    }
}
