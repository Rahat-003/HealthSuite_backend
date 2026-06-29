package com.healthsuite.scraper.service;

import com.healthsuite.scraper.dto.DoctorScrapedData;

import java.util.List;

public interface ScraperStrategy {

    boolean supports(String url);

    List<DoctorScrapedData> scrape(String url);
}
