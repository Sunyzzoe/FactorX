package com.factorx.model;

public record FactorScore(
        String name,
        double rawScore,
        double threshold,
        double activation,
        double weight,
        double contribution,
        String reason
) {}
