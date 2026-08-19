package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "event_companies")
public class EventCompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(length = 32)
    private String symbol;

    @Column(name = "relation_type", nullable = false, length = 32)
    private String relationType;

    @Column(name = "relevance_score", nullable = false, precision = 6, scale = 5)
    private BigDecimal relevanceScore;

    protected EventCompanyEntity() {
    }

    public EventCompanyEntity(
            Long eventId,
            String companyName,
            String symbol,
            String relationType,
            BigDecimal relevanceScore
    ) {
        this.eventId = eventId;
        this.companyName = companyName;
        this.symbol = symbol;
        this.relationType = relationType;
        this.relevanceScore = relevanceScore;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getRelationType() {
        return relationType;
    }

    public BigDecimal getRelevanceScore() {
        return relevanceScore;
    }
}
