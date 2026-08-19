package com.factorx.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("postgres")
@ConditionalOnProperty(prefix = "factorx.news", name = "enabled", havingValue = "true")
public class NewsPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsPollingScheduler.class);

    private final Map<String, NewsSourceAdapter> adapters;
    private final NewsIngestionService ingestionService;
    private final Map<String, Instant> lastSuccessfulFetch = new ConcurrentHashMap<>();
    private final int maxAttempts;

    public NewsPollingScheduler(
            java.util.List<NewsSourceAdapter> adapters,
            NewsIngestionService ingestionService,
            @Value("${factorx.news.max-fetch-attempts:3}") int maxAttempts
    ) {
        this.adapters = adapters.stream()
                .collect(java.util.stream.Collectors.toMap(NewsSourceAdapter::sourceCode, adapter -> adapter));
        this.ingestionService = ingestionService;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${factorx.news.poll-interval-ms:300000}")
    public void pollNewsSources() {
        adapters.values().forEach(this::poll);
    }

    private void poll(NewsSourceAdapter adapter) {
        Instant since = lastSuccessfulFetch.getOrDefault(adapter.sourceCode(), Instant.now().minusSeconds(900));
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var items = adapter.fetch(since);
                int accepted = 0;
                for (RawNewsItem item : items) {
                    var article = ingestionService.insertIfAbsent(item);
                    if (article != null) {
                        accepted++;
                        ingestionService.analyzeAsync(article.getId());
                    }
                }
                lastSuccessfulFetch.put(adapter.sourceCode(), Instant.now());
                log.info("新闻抓取完成 source={}, fetched={}, accepted={}",
                        adapter.sourceCode(), items.size(), accepted);
                return;
            } catch (NewsSourceException ex) {
                if (!ex.retryable() || attempt == maxAttempts) {
                    log.warn("新闻源抓取失败 source={}, attempts={}, retryable={}, message={}",
                            adapter.sourceCode(), attempt, ex.retryable(), ex.getMessage());
                    return;
                }
                backoff(adapter.sourceCode(), attempt, ex);
            } catch (Exception ex) {
                log.error("新闻源抓取出现未处理异常 source={}", adapter.sourceCode(), ex);
                return;
            }
        }
    }

    private void backoff(String sourceCode, int attempt, NewsSourceException ex) {
        long delayMs = 500L * (1L << (attempt - 1));
        log.warn("新闻源限流或临时失败 source={}, attempt={}, retryInMs={}, message={}",
                sourceCode, attempt, delayMs, ex.getMessage());
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
