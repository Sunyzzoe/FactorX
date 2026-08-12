package com.factorx.model;

public record StockImpact(
        String symbol,
        String company,
        String relation,
        String direction,
        int probability,
        String estimatedMove,
        String horizon,
        double relevance
) {}
