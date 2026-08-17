package com.factorx.model;

public record ReluMetrics(
        double threshold,
        int lookbackDays,
        double reluSlope,
        double positiveDensity,
        double plateauRatio,
        double momentumPurity
) {}
