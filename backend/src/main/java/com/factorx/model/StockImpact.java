package com.factorx.model;

import java.util.List;

public record StockImpact(
        String symbol,
        String company,
        String relation,
        String direction,
        int probability,
        String estimatedMove,
        String horizon,
        double relevance,
        double finalImpactScore,
        List<FactorScore> factors,
        List<ReluMomentumPoint> reluMomentum,
        ReluMetrics reluMetrics
) {}
