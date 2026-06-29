package com.healthsuite.scraper.service;

import com.healthsuite.scraper.dto.DoctorScrapedData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class JsoupScraperStrategy implements ScraperStrategy {

    @Override
    public boolean supports(String url) {
        // Default strategy — handles all static HTML pages
        return true;
    }

    @Override
    public List<DoctorScrapedData> scrape(String url) {
        List<DoctorScrapedData> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; HealthSuiteBot/1.0)")
                    .timeout(30_000)
                    .get();

            // Generic extraction — targets common doctor listing patterns
            // Each site may require specific CSS selectors configured via ScraperConfig
            Elements doctorCards = doc.select(".doctor-card, .doctor-profile, [class*='doctor']");

            if (doctorCards.isEmpty()) {
                // Fallback: look for structured data
                doctorCards = doc.select("article, .card, .listing-item");
            }

            for (Element card : doctorCards) {
                DoctorScrapedData data = extractFromElement(card);
                if (data.getFullName() != null && !data.getFullName().isBlank()) {
                    results.add(data);
                }
            }

            log.info("JSoup scraped {} doctor records from {}", results.size(), url);
        } catch (IOException e) {
            log.error("JSoup scrape failed for {}: {}", url, e.getMessage());
        }
        return results;
    }

    private DoctorScrapedData extractFromElement(Element el) {
        return DoctorScrapedData.builder()
                .fullName(extractText(el, "[class*='name'], h2, h3"))
                .specialty(extractText(el, "[class*='specialty'], [class*='department']"))
                .chamberAddress(extractText(el, "[class*='address'], [class*='chamber']"))
                .hospitalName(extractText(el, "[class*='hospital'], [class*='clinic']"))
                .phoneNumber(extractText(el, "[class*='phone'], [class*='contact']"))
                .profileUrl(extractHref(el, "a[href]"))
                .bmdcNumber(extractBmdc(el))
                .build();
    }

    private String extractText(Element el, String cssQuery) {
        Element found = el.selectFirst(cssQuery);
        return found != null ? found.text().trim() : null;
    }

    private String extractHref(Element el, String cssQuery) {
        Element found = el.selectFirst(cssQuery);
        return found != null ? found.attr("abs:href") : null;
    }

    private String extractBmdc(Element el) {
        String text = el.text();
        // Look for "BMDC: 12345" or "Reg. No.: 12345" patterns
        var matcher = java.util.regex.Pattern.compile("(?:BMDC|Reg\\.?\\s*No\\.?)\\s*:?\\s*(\\w+)",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
