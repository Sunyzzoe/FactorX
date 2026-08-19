package com.factorx.news;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewsTextNormalizerTest {

    private final NewsTextNormalizer normalizer = new NewsTextNormalizer();

    @Test
    void removesMarkupAndNormalizesWhitespace() {
        assertEquals("Tesla wins a contract", normalizer.text(" Tesla <b>wins</b>\n a   contract "));
    }

    @Test
    void removesCommonTrackingParametersFromUrls() {
        assertEquals(
                "https://example.com/news?id=7",
                normalizer.url("https://example.com/news?id=7&utm_source=rss&utm_campaign=launch")
        );
    }

    @Test
    void producesSameHashForEquivalentCrossSourceContent() {
        RawNewsItem first = new RawNewsItem(
                "1", "Tesla update", "Tesla wins a contract", "Reuters", "rss",
                "https://example.com/a", Instant.parse("2026-08-19T00:00:00Z"), "en", null, null
        );
        RawNewsItem second = new RawNewsItem(
                "2", " Tesla update ", "Tesla  wins\n a contract", "Yahoo Finance", "finnhub",
                "https://example.org/b", Instant.parse("2026-08-19T00:00:00Z"), "en", null, null
        );

        assertEquals(normalizer.hash(first), normalizer.hash(second));
    }
}
