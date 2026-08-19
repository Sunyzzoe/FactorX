package com.factorx.news;

import java.time.Instant;

public record NormalizedNews(
        String externalId,
        String title,
        String body,
        String source,
        String sourceCode,
        String url,
        Instant publishedAt,
        Instant fetchedAt,
        String language,
        String region,
        String sectorHint,
        String contentHash
) {
}
