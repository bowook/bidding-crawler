package com.recokr.internship.crawler.infrastructure.playwright;

import com.google.gson.JsonObject;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.recokr.internship.crawler.bid.application.BidDataService;
import com.recokr.internship.crawler.bid.infrastructure.playwright.NuriApiScanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NuriApiScannerTest {

    private static final String SITE_NAME = "NURI";
    private static final String BASE_URL = "https://nuri.g2b.go.kr";

    @Mock
    private BidDataService bidDataService;

    @Mock
    private Page page;

    @Mock
    private APIRequestContext apiRequestContext;

    @Mock
    private APIResponse apiResponse;

    private NuriApiScanner nuriApiScanner;

    @BeforeEach
    void setUp() {
        nuriApiScanner = new NuriApiScanner(
                bidDataService,
                SITE_NAME,
                BASE_URL
        );
    }

    private APIResponse createMockResponse(int status, String text) {
        APIResponse response = mock(APIResponse.class);
        when(response.status()).thenReturn(status);
        when(response.text()).thenReturn(text);

        return response;
    }

    @DisplayName("supports 메서드는")
    @Nested
    class DescribeSupports {

        @DisplayName("대소문자 구분 없이 사이트명 확인한다.")
        @ParameterizedTest
        @ValueSource(strings = {"NURI", "Nuri", "nuri"})
        void supports_CaseInsensitive_ReturnsTrue(String siteName) {
            assertThat(nuriApiScanner.supports(siteName)).isTrue();
        }

        @DisplayName("다른 사이트명이 들어오면 false를 반환한다")
        @Test
        void supports_ValidSiteName_ReturnsTrue() {
            // given
            String invalidSiteName = "bowook";

            // when
            boolean result = nuriApiScanner.supports(invalidSiteName);

            // then
            assertThat(result).isFalse();
        }
    }

    @DisplayName("scan 메서드는")
    @Nested
    class Describe_scan {

        @Test
        @DisplayName("정상적인 API 응답이 오면 데이터를 수집하고 저장한다")
        void it_collects_and_saves_data_when_response_is_ok() {
            // given
            String startDate = "20240101";
            String endDate = "20240131";
            String expectedBidNo = "20240208-001";

            when(page.request()).thenReturn(apiRequestContext);

            // URL에 따라 다른 응답을 반환하도록 설정 (기존과 동일)
            when(apiRequestContext.post(anyString(), any())).thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                if (url.contains("selectBidPbancList.do")) {
                    return createMockResponse(200, """
                            { "result": [{ "bidPbancNo": "%s", "bidPbancOrd": "00", "nextRowYn": "N" }] }
                            """.formatted(expectedBidNo));
                } else if (url.contains("selectBidPbancPrgsDetl.do")) {
                    return createMockResponse(200, """
                            { "result": { "pbancOrgMap": { "untyAtchFileNo": "FILE_001" }, "bidPbancNo": "%s" } }
                            """.formatted(expectedBidNo));
                } else if (url.contains("selectUntyAtchFileList.do")) {
                    return createMockResponse(200, "{ \"dlUntyAtchFileL\": [] }");
                }
                return createMockResponse(404, "{}");
            });

            // 1. 캡터 선언
            ArgumentCaptor<JsonObject> collectCaptor = ArgumentCaptor.forClass(JsonObject.class);
            ArgumentCaptor<String> startDtCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> endDtCaptor = ArgumentCaptor.forClass(String.class);

            // when
            nuriApiScanner.scan(page, startDate, endDate);

            // then
            assertAll("수집 및 저장 데이터 검증",
                    () -> verify(bidDataService, atLeastOnce()).clearStorage(),
                    () -> {
                        verify(bidDataService, atLeastOnce()).collect(collectCaptor.capture());
                        JsonObject capturedData = collectCaptor.getValue();
                        assertThat(capturedData.get("bidPbancNo").getAsString()).isEqualTo(expectedBidNo);
                        assertThat(capturedData.has("attachedFileList")).isTrue();
                    },
                    () -> {
                        verify(bidDataService).save(startDtCaptor.capture(), endDtCaptor.capture());
                        assertThat(startDtCaptor.getValue()).isEqualTo(startDate);
                        assertThat(endDtCaptor.getValue()).isEqualTo(endDate);
                    }
            );
        }

        @Test
        @DisplayName("날짜 파라미터가 비어있으면 기본값(1개월 전 ~ 오늘)으로 동작한다")
        void it_uses_default_date_when_params_are_null() {
            // given
            when(page.request()).thenReturn(apiRequestContext);
            when(apiRequestContext.post(anyString(), any())).thenReturn(apiResponse);
            when(apiResponse.status()).thenReturn(500);

            ArgumentCaptor<String> startDtCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> endDtCaptor = ArgumentCaptor.forClass(String.class);

            // when
            nuriApiScanner.scan(page, null, "");

            // then
            String expectedEndDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String expectedStartDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            assertAll("기본 날짜 계산 검증",
                    () -> verify(bidDataService).save(startDtCaptor.capture(), endDtCaptor.capture()),
                    () -> assertThat(startDtCaptor.getValue()).isEqualTo(expectedStartDate),
                    () -> assertThat(endDtCaptor.getValue()).isEqualTo(expectedEndDate)
            );
        }

        @Test
        @DisplayName("API 응답에 결과가 없으면 즉시 종료한다")
        void it_stops_when_no_result_found() {
            // given
            when(page.request()).thenReturn(apiRequestContext);
            when(apiRequestContext.post(anyString(), any())).thenReturn(apiResponse);
            when(apiResponse.status()).thenReturn(200);
            when(apiResponse.text()).thenReturn("{\"result\": []}");

            // when
            nuriApiScanner.scan(page, "20240101", "20240131");

            // then
            assertAll("빈 결과 처리 검증",
                    () -> verify(bidDataService, never()).collect(any()),
                    () -> verify(bidDataService).save(anyString(), anyString())
            );
        }
    }
}
