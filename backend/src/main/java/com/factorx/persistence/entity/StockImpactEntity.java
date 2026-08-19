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
@Table(name = "stock_impacts")
public class StockImpactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal probability;

    @Column(name = "estimated_low", precision = 12, scale = 4)
    private BigDecimal estimatedLow;

    @Column(name = "estimated_high", precision = 12, scale = 4)
    private BigDecimal estimatedHigh;

    @Column(length = 64)
    private String horizon;

    @Column(name = "relevance_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal relevanceScore;

    @Column(name = "final_impact_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal finalImpactScore;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "risk_note", columnDefinition = "text")
    private String riskNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockImpactEntity() {
    }

    public StockImpactEntity(
            Long eventId,
            String symbol,
            String companyName,
            String direction,
            BigDecimal probability,
            BigDecimal estimatedLow,
            BigDecimal estimatedHigh,
            String horizon,
            BigDecimal relevanceScore,
            BigDecimal finalImpactScore,
            String explanation,
            String riskNote
    ) {
        this.eventId = eventId;
        this.symbol = symbol;
        this.companyName = companyName;
        this.direction = direction;
        this.probability = probability;
        this.estimatedLow = estimatedLow;
        this.estimatedHigh = estimatedHigh;
        this.horizon = horizon;
        this.relevanceScore = relevanceScore;
        this.finalImpactScore = finalImpactScore;
        this.explanation = explanation;
        this.riskNote = riskNote;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public BigDecimal getEstimatedLow() {
        return estimatedLow;
    }

    public BigDecimal getEstimatedHigh() {
        return estimatedHigh;
    }

    public String getHorizon() {
        return horizon;
    }

    public BigDecimal getRelevanceScore() {
        return relevanceScore;
    }

    public BigDecimal getFinalImpactScore() {
        return finalImpactScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getRiskNote() {
        return riskNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
