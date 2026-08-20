package com.factorx.market.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockPrice(
        String symbol,
        LocalDate tradeDate,
        BigDecimal closePrice,
        BigDecimal adjustedClosePrice,
        Long volume,
        BigDecimal returnPct,
        String currency,
        String provider
) {}
