package com.factorx.news;

import com.factorx.model.AnalysisRequest;
import com.factorx.persistence.entity.NewsArticleEntity;
import com.factorx.persistence.repository.NewsArticleRepository;
import com.factorx.service.NewsAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Profile("postgres")
public class NewsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(NewsIngestionService.class);

    private final NewsArticleRepository repository;
    private final NewsTextNormalizer normalizer;
    private final NewsAnalysisService analysisService;

    public NewsIngestionService(
            NewsArticleRepository repository,
            NewsTextNormalizer normalizer,
            NewsAnalysisService analysisService
    ) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.analysisService = analysisService;
    }

    @Transactional
    public NewsArticleEntity insertIfAbsent(RawNewsItem raw) {
        NormalizedNews news = normalize(raw);
        if (news == null) {
            return null;
        }
        if (!news.externalId().isBlank()) {
            var existing = repository.findBySourceCodeAndExternalId(news.sourceCode(), news.externalId());
            if (existing.isPresent()) {
                return null;
            }
        }
        if (!news.url().isBlank() && repository.findByUrl(news.url()).isPresent()) {
            return null;
        }
        if (repository.findByContentHash(news.contentHash()).isPresent()) {
            return null;
        }

        NewsArticleEntity entity = new NewsArticleEntity(
                news.title(),
                news.source(),
                news.body(),
                news.contentHash(),
                news.publishedAt()
        );
        entity.applyMetadata(
                news.sourceCode(),
                news.externalId(),
                news.url(),
                news.language(),
                news.region(),
                news.sectorHint()
        );
        return repository.save(entity);
    }

    @Async
    public void analyzeAsync(Long articleId) {
        repository.findById(articleId).ifPresent(article -> {
            try {
                article.markAnalyzing();
                repository.save(article);
                analysisService.analyze(new AnalysisRequest(
                        article.getTitle(),
                        article.getSource(),
                        article.getBody()
                ));
                article.markAnalyzed();
                repository.save(article);
            } catch (Exception ex) {
                article.markFailed(trimError(ex));
                repository.save(article);
                log.error("新闻分析失败 articleId={}, retryCount={}", articleId, article.getRetryCount(), ex);
            }
        });
    }

    @Async
    public void retryFailed(Long articleId) {
        repository.findById(articleId).ifPresent(article -> {
            if ("FAILED".equals(article.getStatus()) && article.getRetryCount() < 3) {
                analyzeAsync(articleId);
            }
        });
    }

    private NormalizedNews normalize(RawNewsItem raw) {
        String title = normalizer.text(raw.title());
        String body = normalizer.text(raw.body());
        String url = normalizer.url(raw.url());
        if (title.isBlank() || url.isBlank() || raw.publishedAt() == null) {
            return null;
        }
        RawNewsItem cleaned = new RawNewsItem(
                normalizer.text(raw.externalId()),
                title,
                body,
                normalizer.text(raw.source()),
                normalizer.text(raw.sourceCode()),
                url,
                raw.publishedAt(),
                normalizer.text(raw.language()),
                normalizer.text(raw.region()),
                normalizer.text(raw.sectorHint())
        );
        return new NormalizedNews(
                cleaned.externalId(),
                cleaned.title(),
                cleaned.body(),
                cleaned.source(),
                cleaned.sourceCode(),
                cleaned.url(),
                cleaned.publishedAt(),
                Instant.now(),
                cleaned.language(),
                cleaned.region(),
                cleaned.sectorHint(),
                normalizer.hash(cleaned)
        );
    }

    private String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
