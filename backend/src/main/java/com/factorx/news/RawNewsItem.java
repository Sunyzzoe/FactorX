package com.factorx.news;

import java.time.Instant;

public record RawNewsItem(
        String externalId,
        String title,
        String body,
        String source,
        String sourceCode,
        String url,
        Instant publishedAt,
        String language,
        String region,
        String sectorHint
) {
}
