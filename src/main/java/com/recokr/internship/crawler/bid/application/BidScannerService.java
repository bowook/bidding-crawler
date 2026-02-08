package com.recokr.internship.crawler.bid.application;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.recokr.internship.crawler.bid.domain.BidScanner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BidScannerService {
    private final BidScannerRegistry scannerRegistry;
    private final Browser browser;

    public void crawl(final String targetSite, final String startDate, final String endDate) {
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            BidScanner scanner = scannerRegistry.getScanner(targetSite);
            scanner.scan(page, startDate, endDate);

        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage());
        }
    }
}
