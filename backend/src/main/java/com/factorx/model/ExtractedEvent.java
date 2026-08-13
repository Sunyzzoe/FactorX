package com.factorx.model;

import java.util.List;

public record ExtractedEvent(
        String eventType,
        String sector,
        String country,
        Long projectAmountUsd,
        List<String> companies,
        List<String> keywords,
        String source,
        double sourceCredibility
) {}
