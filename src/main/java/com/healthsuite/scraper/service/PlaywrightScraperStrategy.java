package com.healthsuite.scraper.service;

import com.healthsuite.scraper.dto.DoctorScrapedData;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Playwright-based scraper for JS-heavy, client-side rendered doctor directories.
 * Falls back gracefully if Playwright browsers are not installed in the runtime environment.
 */
@Component
@Slf4j
public class PlaywrightScraperStrategy implements ScraperStrategy {

    // Marker in URL to indicate Playwright is required (or configure per-URL in ScraperConfig)
    private static final String PLAYWRIGHT_MARKER = "?scraper=playwright";

    private volatile com.microsoft.playwright.Playwright playwright;
    private volatile com.microsoft.playwright.Browser browser;
    private volatile boolean initialized = false;
    private volatile boolean available = false;

    private synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            playwright = com.microsoft.playwright.Playwright.create();
            browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            available = true;
            log.info("Playwright Chromium browser initialized for scraper");
        } catch (Exception e) {
            log.warn("Playwright not available — will skip JS-heavy pages: {}", e.getMessage());
            available = false;
        }
    }

    @Override
    public boolean supports(String url) {
        return url.contains(PLAYWRIGHT_MARKER);
    }

    @Override
    public List<DoctorScrapedData> scrape(String url) {
        ensureInitialized();
        if (!available) {
            log.warn("Playwright unavailable — skipping JS scrape for: {}", url);
            return List.of();
        }

        List<DoctorScrapedData> results = new ArrayList<>();
        String cleanUrl = url.replace(PLAYWRIGHT_MARKER, "");

        try (com.microsoft.playwright.BrowserContext context = browser.newContext()) {
            com.microsoft.playwright.Page page = context.newPage();
            page.navigate(cleanUrl);
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

            // Extract doctor names from the rendered DOM
            List<String> names = page.locator("[class*='doctor'] [class*='name'], h2, h3")
                    .allTextContents();

            for (String name : names) {
                if (!name.isBlank()) {
                    results.add(DoctorScrapedData.builder().fullName(name.trim()).build());
                }
            }

            log.info("Playwright scraped {} records from {}", results.size(), cleanUrl);
        } catch (Exception e) {
            log.error("Playwright scrape failed for {}: {}", cleanUrl, e.getMessage());
        }

        return results;
    }

    @PreDestroy
    public void close() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
        }
    }
}
