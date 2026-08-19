package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "analysis_id", nullable = false, unique = true)
    private Long analysisId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(length = 64)
    private String sector;

    @Column(length = 128)
    private String country;

    @Column(name = "project_amount_usd", precision = 20, scale = 2)
    private BigDecimal projectAmountUsd;

    @Column(name = "source_credibility", nullable = false, precision = 6, scale = 5)
    private BigDecimal sourceCredibility;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EventEntity() {
    }

    public EventEntity(
            Long analysisId,
            String eventType,
            String sector,
            String country,
            BigDecimal projectAmountUsd,
            BigDecimal sourceCredibility
    ) {
        this.analysisId = analysisId;
        this.eventType = eventType;
        this.sector = sector;
        this.country = country;
        this.projectAmountUsd = projectAmountUsd;
        this.sourceCredibility = sourceCredibility;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSector() {
        return sector;
    }

    public String getCountry() {
        return country;
    }

    public BigDecimal getProjectAmountUsd() {
        return projectAmountUsd;
    }

    public BigDecimal getSourceCredibility() {
        return sourceCredibility;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
