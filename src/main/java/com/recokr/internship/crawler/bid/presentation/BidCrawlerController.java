package com.recokr.internship.crawler.bid.presentation;

import com.recokr.internship.crawler.bid.application.BidScannerService;
import com.recokr.internship.crawler.bid.presentation.api.BidCrawlerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/crawl")
@RestController
public class BidCrawlerController implements BidCrawlerApi {

    private final BidScannerService bidScannerService;

    @Override
    @GetMapping
    public String triggerCrawl(
            @RequestParam final String targetSite,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        bidScannerService.crawl(targetSite, startDate, endDate);

        return String.format(
                "[%s] 크롤링이 시작되었습니다.\n" +
                        "최종 결과물 파일: 프로젝트 루트의 /output/nuri_crawl_results.json",
                targetSite
        );
    }
}
