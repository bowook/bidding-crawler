package com.recokr.internship.crawler.bid.application;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.recokr.internship.crawler.bid.infrastructure.file.BidFileWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BidDataService {

    private final BidFileWriter bidFileWriter;
    private JsonArray storage = new JsonArray();

    public void clearStorage() {
        this.storage = new JsonArray();
    }

    public void collect(final JsonObject detailedData) {
        if (detailedData != null) {
            storage.add(detailedData);
        }
    }

    public void save(final String startDate, final String endDate) {
        bidFileWriter.writeResults(storage, startDate, endDate);
    }
}
