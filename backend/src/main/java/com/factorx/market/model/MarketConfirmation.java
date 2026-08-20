package com.factorx.market.model;

public record MarketConfirmation(
        double score,
        boolean priceConfirmed,
        boolean volumeConfirmed,
        boolean sectorConfirmed,
        boolean conflict,
        String industryEtf,
        Double industryEtfReturn1d,
        String riskNote
) {}
