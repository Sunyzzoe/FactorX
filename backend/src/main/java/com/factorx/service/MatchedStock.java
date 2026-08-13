package com.factorx.service;

public record MatchedStock(
        String symbol,
        String company,
        String relation,
        double relevance
) {}
