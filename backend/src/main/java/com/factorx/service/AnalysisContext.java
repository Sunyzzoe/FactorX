package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;
import com.factorx.model.StockImpact;

import java.util.List;

public class AnalysisContext {
    private final AnalysisRequest request;
    private ExtractedEvent event;
    private List<MatchedStock> matchedStocks = List.of();
    private ReluResult reluResult;
    private List<StockImpact> stockImpacts = List.of();
    private String explanation = "";
    private String riskNote = "";

    public AnalysisContext(AnalysisRequest request) {
        this.request = request;
    }

    public AnalysisRequest request() {
        return request;
    }

    public ExtractedEvent event() {
        return event;
    }

    public void event(ExtractedEvent event) {
        this.event = event;
    }

    public List<MatchedStock> matchedStocks() {
        return matchedStocks;
    }

    public void matchedStocks(List<MatchedStock> matchedStocks) {
        this.matchedStocks = matchedStocks;
    }

    public ReluResult reluResult() {
        return reluResult;
    }

    public void reluResult(ReluResult reluResult) {
        this.reluResult = reluResult;
    }

    public List<ReluMomentumPoint> reluMomentum() {
        return reluResult == null ? List.of() : reluResult.momentum();
    }

    public List<ReluFactor> reluFactors() {
        return reluResult == null ? List.of() : reluResult.factors();
    }

    public List<StockImpact> stockImpacts() {
        return stockImpacts;
    }

    public void stockImpacts(List<StockImpact> stockImpacts) {
        this.stockImpacts = stockImpacts;
    }

    public String explanation() {
        return explanation;
    }

    public void explanation(String explanation) {
        this.explanation = explanation;
    }

    public String riskNote() {
        return riskNote;
    }

    public void riskNote(String riskNote) {
        this.riskNote = riskNote;
    }
}
