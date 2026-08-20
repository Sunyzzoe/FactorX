package com.factorx.backtest;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PredictRequest(@NotBlank String modelVersion, Map<String, Double> features) { }
