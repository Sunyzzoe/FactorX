package com.factorx.model;

public record ReluMomentumPoint(
        int day,
        double price,
        double returnPct,
        double cumulativeReturn,
        double reluReturn,
        double reluMomentum
) {}
