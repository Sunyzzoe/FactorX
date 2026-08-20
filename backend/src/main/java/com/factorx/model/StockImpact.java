package com.factorx.model;

import java.util.List;
import com.factorx.market.model.MarketConfirmation;
import com.factorx.market.model.MarketIndicators;

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
        ReluMetrics reluMetrics,
        String marketDataStatus,
        MarketIndicators marketData,
        MarketConfirmation marketConfirmation
) {}
