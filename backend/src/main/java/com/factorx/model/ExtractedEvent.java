package com.factorx.model;

import java.util.List;

public record ExtractedEvent(
        String eventType,
        String sector,
        String country,
        long projectAmountUsd,
        List<String> companies,
        String source,
        double sourceCredibility
) {}
