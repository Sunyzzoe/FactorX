package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StockMatcherService {

    private static final Map<String, StockProfile> STOCKS = new LinkedHashMap<>();

    static {
        STOCKS.put("TSLA", new StockProfile("TSLA", "Tesla", "新能源",
                List.of("tesla", "tsla", "ev", "battery", "solar", "energy storage")));
        STOCKS.put("NVDA", new StockProfile("NVDA", "Nvidia", "AI 芯片",
                List.of("nvidia", "nvda", "gpu", "ai chip", "data center")));
        STOCKS.put("AMD", new StockProfile("AMD", "AMD", "AI 芯片",
                List.of("amd", "gpu", "ai chip", "semiconductor")));
        STOCKS.put("ASML", new StockProfile("ASML", "ASML", "半导体",
                List.of("asml", "lithography", "semiconductor equipment")));
        STOCKS.put("TSM", new StockProfile("TSM", "TSMC", "半导体",
                List.of("tsmc", "taiwan semiconductor", "foundry", "chip manufacturing")));
        STOCKS.put("ENPH", new StockProfile("ENPH", "Enphase", "新能源",
                List.of("enphase", "inverter", "solar")));
    }

    public List<MatchedStock> match(ExtractedEvent event, AnalysisRequest request) {
        String text = (request.headline() + " " + nullToEmpty(request.body())).toLowerCase(Locale.ROOT);
        List<MatchedStock> matches = new ArrayList<>();

        for (StockProfile stock : STOCKS.values()) {
            boolean directCompany = event.companies().stream()
                    .anyMatch(company -> company.equalsIgnoreCase(stock.company()));
            boolean keywordHit = stock.keywords().stream().anyMatch(text::contains);
            boolean sectorHit = stock.sector().equals(event.sector());

            if (directCompany || keywordHit || sectorHit) {
                double relevance = directCompany ? 0.92 : keywordHit ? 0.74 : 0.56;
                String relation = directCompany ? "直接相关" : keywordHit ? "产业链相关" : "行业相关";
                matches.add(new MatchedStock(stock.symbol(), stock.company(), relation, relevance));
            }
        }

        if (matches.isEmpty()) {
            matches.add(new MatchedStock("TSLA", "Tesla", "待确认", 0.45));
        }

        return matches.stream()
                .sorted(Comparator.comparingDouble(MatchedStock::relevance).reversed())
                .limit(4)
                .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record StockProfile(String symbol, String company, String sector, List<String> keywords) {}
}
