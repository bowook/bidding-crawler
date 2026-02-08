package com.recokr.internship.crawler.bid.presentation.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Crawl", description = "입찰 공고 크롤링 제어 API")
public interface BidCrawlerApi {

    @Operation(
            summary = "크롤링 실행 트리거",
            description = "특정 사이트와 기간을 지정하여 입찰 공고 데이터를 수집합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "크롤링 요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 발생")
    })
    String triggerCrawl(
            @Parameter(description = "대상 사이트 명 (예: NURI)", example = "NURI")
            String targetSite,

            @Parameter(description = "수집 시작일 (YYYYMMDD 형식)", example = "20240101")
            String startDate,

            @Parameter(description = "수집 종료일 (YYYYMMDD 형식)", example = "20240131")
            String endDate
    );
}
