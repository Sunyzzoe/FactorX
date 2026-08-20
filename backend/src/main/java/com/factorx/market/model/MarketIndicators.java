package com.factorx.market.model;

public record MarketIndicators(
        Double return1d,
        Double return5d,
        Double volume,
        Double averageVolume20d,
        Double volumeRatio,
        Double volatility20d,
        Double annualizedVolatility
) {}
