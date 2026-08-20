package com.factorx.backtest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BacktestRequest(
        @NotBlank String startDate,
        @NotBlank String endDate,
        List<String> symbols,
        @NotEmpty List<Integer> horizons,
        Double transactionCost,
        Double slippage
) {
    public LocalDate start() { return LocalDate.parse(startDate); }
    public LocalDate end() { return LocalDate.parse(endDate); }
    public double cost() { return transactionCost == null ? 0 : transactionCost; }
    public double slip() { return slippage == null ? 0 : slippage; }
}
