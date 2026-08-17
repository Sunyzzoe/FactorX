package com.factorx.model;

import java.util.List;

public record AnalysisResponse(
        String analyzedAt,
        ExtractedEvent event,
        List<StockImpact> stocks,
        List<ReluMomentumPoint> reluMomentum,
        ReluMetrics reluMetrics,
        List<ReluFactor> reluFactors,
        String explanation,
        String riskNote
) {}
