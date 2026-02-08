package com.recokr.internship.crawler.bid.infrastructure.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BidFileWriter {

    private final String basePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public BidFileWriter(@Value("${app.crawl.output-path:./output/}") String basePath) {
        this.basePath = basePath.endsWith(File.separator) ? basePath : basePath + File.separator;
    }

    public String writeResults(final JsonArray data, final String startDate, final String endDate) {
        String fileName = createFileName(startDate, endDate);
        File targetFile = prepareTargetFile(fileName);

        return writeJsonToFile(data, targetFile);
    }

    private String createFileName(final String startDate, final String endDate) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        return String.format("nuri_bids_%s_%s_%s.json", startDate, endDate, timestamp);
    }

    private File prepareTargetFile(final String fileName) {
        File directory = new File(basePath);
        if (!directory.exists() && directory.mkdirs()) {
            log.info("저장 디렉토리 생성됨: {}", basePath);
        }

        return new File(directory, fileName);
    }

    private String writeJsonToFile(final JsonArray data, final File targetFile) {
        String fullPath = targetFile.getAbsolutePath();

        try (FileWriter writer = new FileWriter(targetFile)) {
            gson.toJson(data, writer);
            log.info("JSON 산출물 저장 완료: {}", fullPath);

            return fullPath;
        } catch (IOException e) {
            log.error("파일 저장 실패 (경로: {}): {}", fullPath, e.getMessage());

            return "저장 실패";
        }
    }
}
