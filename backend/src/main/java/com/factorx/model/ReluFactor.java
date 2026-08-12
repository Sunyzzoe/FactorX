package com.factorx.model;

public record ReluFactor(
        String name,
        double rawScore,
        double threshold,
        double activation,
        String reason
) {}
