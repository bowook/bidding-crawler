package com.recokr.internship.crawler.bid.infrastructure.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Playwright;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaywrightConfig {

    @Bean(destroyMethod = "close")
    public Playwright playwright() {
        return Playwright.create();
    }

    @Bean(destroyMethod = "close")
    public Browser browser(final Playwright playwright) {
        return playwright.chromium()
                .launch(new LaunchOptions().setHeadless(true)
                        .setArgs(List.of("--no-sandbox")));
    }
}
