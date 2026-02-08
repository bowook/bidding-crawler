package com.recokr.internship.crawler.bid.application;

import com.recokr.internship.crawler.bid.domain.BidScanner;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BidScannerRegistry {

    private final List<BidScanner> scanners;

    public BidScanner getScanner(final String siteName) {
        return scanners.stream()
                .filter(s -> s.supports(siteName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 사이트입니다."));
    }
}
