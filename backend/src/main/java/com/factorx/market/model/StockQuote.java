package com.factorx.market.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuote(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal dayOpen,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        Long volume,
        Instant quoteTime,
        String currency,
        String provider
) {}
