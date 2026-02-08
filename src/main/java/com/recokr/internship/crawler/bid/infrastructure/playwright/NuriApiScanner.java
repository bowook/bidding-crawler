package com.recokr.internship.crawler.bid.infrastructure.playwright;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.RequestOptions;
import com.recokr.internship.crawler.bid.application.BidDataService;
import com.recokr.internship.crawler.bid.domain.BidScanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NuriApiScanner implements BidScanner {

    private final BidDataService bidDataService;
    private final String siteName;
    private final String baseUrl;

    public NuriApiScanner(
            BidDataService bidDataService,
            @Value("${nuri.site-name}") String siteName,
            @Value("${nuri.base-url}") String baseUrl
    ) {
        this.bidDataService = bidDataService;
        this.siteName = siteName;
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean supports(final String targetSiteName) {
        return siteName.equalsIgnoreCase(targetSiteName);
    }

    @Override
    public void scan(final Page page, final String startDate, final String endDate) {
        bidDataService.clearStorage();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate now = LocalDate.now();
        String finalStartDate =
                (startDate != null && !startDate.isEmpty()) ? startDate : now.minusMonths(1).format(formatter);
        String finalEndDate = (endDate != null && !endDate.isEmpty()) ? endDate : now.format(formatter);
        String oneMonthLater = LocalDate.parse(finalEndDate, formatter).plusMonths(1).format(formatter);

        page.navigate(baseUrl);
        String searchUrl = baseUrl + "/nn/nnb/nnba/selectBidPbancList.do";
        int currentPage = 1;
        boolean hasNextPage = true;

        while (hasNextPage) {
            log.info("누리장터 스캔 중... 기간: {} ~ {}, 페이지: {}", finalStartDate, finalEndDate, currentPage);
            APIResponse searchResponse = requestSearch(page, searchUrl, currentPage, finalStartDate, finalEndDate,
                    oneMonthLater);

            if (searchResponse.status() == 200) {
                JsonObject searchResult = JsonParser.parseString(searchResponse.text()).getAsJsonObject();
                if (!searchResult.has("result") || searchResult.get("result").isJsonNull()) {
                    break;
                }

                JsonArray bidList = searchResult.getAsJsonArray("result");
                if (bidList.isEmpty()) {
                    break;
                }

                for (JsonElement element : bidList) {
                    if (element == null || element.isJsonNull()) {
                        continue;
                    }

                    JsonObject bid = element.getAsJsonObject();
                    // 필수 필드 존재 확인 로직 유지
                    if (bid.has("bidPbancNo") && !bid.get("bidPbancNo").isJsonNull()) {
                        JsonObject detailedBid = fetchDetail(page, bid);
                        bidDataService.collect(detailedBid);
                    }
                }

                JsonObject lastItem = bidList.get(bidList.size() - 1).getAsJsonObject();
                hasNextPage = lastItem.has("nextRowYn") && "Y".equals(lastItem.get("nextRowYn").getAsString());

                if (hasNextPage) {
                    currentPage++;
                    page.waitForTimeout(500);
                }
            } else {
                hasNextPage = false;
            }
        }
        bidDataService.save(finalStartDate, finalEndDate);
        log.info("모든 페이지 스캔 및 파일 생성 완료!");
    }

    private APIResponse requestSearch(
            final Page page, final String searchUrl, final int currentPage,
            final String finalStartDate, final String finalEndDate, final String oneMonthLater
    ) {
        JsonObject dlParamM = new JsonObject();
        dlParamM.addProperty("currentPage", currentPage);
        dlParamM.addProperty("recordCountPerPage", "10");
        dlParamM.addProperty("pbancPstgStDt", finalStartDate);
        dlParamM.addProperty("pbancPstgEdDt", finalEndDate);
        dlParamM.addProperty("onbsPrnmntStDt", finalEndDate);
        dlParamM.addProperty("onbsPrnmntEdDt", oneMonthLater);
        dlParamM.addProperty("pbancPstgYn", "Y");
        addEmptySearchProperties(dlParamM);

        JsonObject requestBody = new JsonObject();
        requestBody.add("dlParamM", dlParamM);

        return page.request().post(searchUrl, RequestOptions.create()
                .setHeader("Content-Type", "application/json;charset=UTF-8")
                .setHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                .setHeader("Menu-Info",
                        "{\"menuNo\":\"15401\",\"menuCangVal\":\"NNBA001_01\",\"bsneClsfCd\":\"%EC%97%85130031\",\"scrnNo\":\"00777\"}")
                .setData(requestBody.toString())
        );
    }

    private JsonObject fetchDetail(Page page, JsonObject bid) {
        String detailUrl = baseUrl + "/nn/nnb/nnbb/selectBidPbancPrgsDetl.do";
        String bidNo = bid.get("bidPbancNo").getAsString();
        String bidOrd = bid.get("bidPbancOrd").getAsString();

        JsonObject dlSrchCndtM = new JsonObject();
        dlSrchCndtM.addProperty("bidPbancNo", bidNo);
        dlSrchCndtM.addProperty("bidPbancOrd", bidOrd);
        dlSrchCndtM.addProperty("bidClsfNo", bid.has("bidClsfNo") ? bid.get("bidClsfNo").getAsString() : "0");
        dlSrchCndtM.addProperty("bidPrgrsOrd", bid.has("bidPrgrsOrd") ? bid.get("bidPrgrsOrd").getAsString() : "000");
        dlSrchCndtM.addProperty("pstNo", bidNo);
        dlSrchCndtM.addProperty("paramGbn", "1");
        addEmptyDetailProperties(dlSrchCndtM);

        JsonObject requestBody = new JsonObject();
        requestBody.add("dlSrchCndtM", dlSrchCndtM);

        APIResponse detailResponse = page.request().post(detailUrl, RequestOptions.create()
                .setHeader("Content-Type", "application/json;charset=UTF-8")
                .setHeader("Menu-Info",
                        "{\"menuNo\":\"12062\",\"menuCangVal\":\"NNBB001_01\",\"bsneClsfCd\":\"%EC%97%85130031\",\"scrnNo\":\"01978\"}")
                .setData(requestBody.toString())
        );

        if (detailResponse.status() == 200) {
            JsonObject resultObj = JsonParser.parseString(detailResponse.text()).getAsJsonObject();
            if (resultObj.get("result").isJsonNull()) {
                return null;
            }

            JsonObject result = resultObj.getAsJsonObject("result");
            JsonObject pbancOrgMap = result.getAsJsonObject("pbancOrgMap");

            String fileNo = (pbancOrgMap != null && !pbancOrgMap.get("untyAtchFileNo").isJsonNull())
                    ? pbancOrgMap.get("untyAtchFileNo").getAsString() : "";

            JsonArray attachedFiles = fetchFileList(page, fileNo);
            result.add("attachedFileList", attachedFiles); // 상세 데이터에 첨부파일 병합

            log.info("상세 정보 수집 성공: {}", bidNo);
            return result;
        }
        return null;
    }

    private JsonArray fetchFileList(Page page, String fileGroupNo) {
        if (fileGroupNo == null || fileGroupNo.isEmpty()) {
            return new JsonArray();
        }

        String fileListUrl = baseUrl + "/fs/fsc/fscb/UntyAtchFile/selectUntyAtchFileList.do";
        JsonObject dlUntyAtchFileM = new JsonObject();
        dlUntyAtchFileM.addProperty("untyAtchFileNo", fileGroupNo);
        dlUntyAtchFileM.addProperty("bsneClsfCd", "업130031");
        dlUntyAtchFileM.addProperty("kuploadId", "wq_uuid_2981_kupload_holder_upload");
        addEmptyFileListProperties(dlUntyAtchFileM);

        JsonObject requestBody = new JsonObject();
        requestBody.add("dlUntyAtchFileM", dlUntyAtchFileM);

        APIResponse response = page.request()
                .post(fileListUrl, RequestOptions.create().setData(requestBody.toString()));
        if (response.status() == 200) {
            JsonObject fileObj = JsonParser.parseString(response.text()).getAsJsonObject();
            return fileObj.has("dlUntyAtchFileL") ? fileObj.getAsJsonArray("dlUntyAtchFileL") : new JsonArray();
        }
        return new JsonArray();
    }

    private void addEmptySearchProperties(final JsonObject obj) {
        String[] keys = {"bidPbancNo", "bidPbancOrd", "bidPbancNm", "prcmBsneSeCd", "bidPbancPgstCd", "bidMthdCd",
                "frgnrRprsvYn", "kbrdrId", "pbancInstUntyGrpNo", "pbancKndCd", "pbancSttsCd", "pdngYn", "rowNum",
                "scsbdMthdCd", "stdCtrtMthdCd", "untyGrpNo", "usrTyCd"};
        for (String key : keys) {
            if (!obj.has(key)) {
                obj.addProperty(key, "");
            }
        }
    }

    private void addEmptyDetailProperties(final JsonObject obj) {
        String[] keys = {"pbancFlag", "bidPbancNm", "bidPbancPgstCd", "flag", "frgnrRprsvYn", "kbrdrId", "odn3ColCn",
                "pbancInstUntyGrpNo", "pbancPstgEdDt", "pbancPstgStDt", "prcmBsneSeCd", "recordCountPerPage", "rowNum",
                "untyGrpNo"};
        for (String key : keys) {
            if (!obj.has(key)) {
                obj.addProperty(key, "");
            }
        }
    }

    private void addEmptyFileListProperties(final JsonObject obj) {
        String[] keys = {"atchFileSqnos", "bsnePath", "atchFileKndCds", "colNm", "ignoreAtchFileKndCds", "imgUrl",
                "kbrdrIds", "tblNm"};
        for (String key : keys) {
            if (!obj.has(key)) {
                obj.addProperty(key, "");
            }
        }
        obj.addProperty("isScanEnabled", false);
        obj.addProperty("viewMode", "view");
        obj.addProperty("webPathUse", "N");
    }
}
