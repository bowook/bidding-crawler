package com.recokr.internship.crawler.bid.domain;

import com.microsoft.playwright.Page;

public interface BidScanner {

    boolean supports(String siteName);

    void scan(Page page, String startDate, String endDate);
}
