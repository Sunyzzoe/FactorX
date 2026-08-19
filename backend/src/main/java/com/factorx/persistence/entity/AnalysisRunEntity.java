package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "analysis_runs")
public class AnalysisRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> parameters = new LinkedHashMap<>();

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected AnalysisRunEntity() {
    }

    public AnalysisRunEntity(Long newsId, String modelVersion, Map<String, Object> parameters) {
        this.newsId = newsId;
        this.status = "RUNNING";
        this.modelVersion = modelVersion;
        this.parameters = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
        this.startedAt = Instant.now();
    }

    public void markSucceeded() {
        this.status = "SUCCEEDED";
        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public String getStatus() {
        return status;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
