package com.factorx.market.model;

import com.factorx.service.ReluResult;

public record MarketSnapshot(
        String status,
        ReluResult reluResult,
        MarketIndicators indicators,
        MarketConfirmation confirmation,
        String provider
) {
    public static MarketSnapshot unavailable() {
        return new MarketSnapshot("UNAVAILABLE", null, null, null, null);
    }
}
