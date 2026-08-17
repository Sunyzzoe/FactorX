package com.factorx.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ReluMomentumRequest(
        @NotEmpty List<@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal> closePrices,
        @NotNull @DecimalMin(value = "0") BigDecimal threshold,
        @NotNull @Positive Integer lookbackDays
) {}
