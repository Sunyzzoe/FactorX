package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "news_articles")
public class NewsArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 80)
    private String source;

    @Column(name = "source_code", length = 40)
    private String sourceCode;

    @Column(name = "external_id", length = 300)
    private String externalId;

    @Column(length = 1000)
    private String url;

    @Column(columnDefinition = "text")
    private String body;

    @Column(length = 16)
    private String language;

    @Column(length = 128)
    private String region;

    @Column(name = "sector_hint", length = 128)
    private String sectorHint;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected NewsArticleEntity() {
    }

    public NewsArticleEntity(
            String title,
            String source,
            String body,
            String contentHash,
            Instant publishedAt
    ) {
        this.title = title;
        this.source = source;
        this.body = body;
        this.contentHash = contentHash;
        this.publishedAt = publishedAt;
        this.createdAt = Instant.now();
        this.fetchedAt = this.createdAt;
        this.status = "RECEIVED";
        this.retryCount = 0;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public String getLanguage() {
        return language;
    }

    public String getRegion() {
        return region;
    }

    public String getSectorHint() {
        return sectorHint;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void applyMetadata(
            String sourceCode,
            String externalId,
            String url,
            String language,
            String region,
            String sectorHint
    ) {
        this.sourceCode = sourceCode;
        this.externalId = externalId;
        this.url = url;
        this.language = language;
        this.region = region;
        this.sectorHint = sectorHint;
    }

    public void markAnalyzing() {
        this.status = "ANALYZING";
        this.lastError = null;
    }

    public void markAnalyzed() {
        this.status = "ANALYZED";
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.retryCount++;
        this.lastError = error;
    }
}
