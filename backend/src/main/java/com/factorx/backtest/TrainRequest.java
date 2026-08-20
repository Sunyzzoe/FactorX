package com.factorx.backtest;

import jakarta.validation.constraints.NotBlank;

public record TrainRequest(
        @NotBlank String startDate,
        @NotBlank String endDate,
        @NotBlank String target,
        String modelType
) { }
