package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "factor_scores")
public class FactorScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "impact_id", nullable = false)
    private Long impactId;

    @Column(name = "factor_name", nullable = false, length = 80)
    private String factorName;

    @Column(name = "raw_score", nullable = false, precision = 8, scale = 5)
    private BigDecimal rawScore;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal threshold;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal activation;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal weight;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal contribution;

    @Column(columnDefinition = "text")
    private String reason;

    protected FactorScoreEntity() {
    }

    public FactorScoreEntity(
            Long impactId,
            String factorName,
            BigDecimal rawScore,
            BigDecimal threshold,
            BigDecimal activation,
            BigDecimal weight,
            BigDecimal contribution,
            String reason
    ) {
        this.impactId = impactId;
        this.factorName = factorName;
        this.rawScore = rawScore;
        this.threshold = threshold;
        this.activation = activation;
        this.weight = weight;
        this.contribution = contribution;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getImpactId() {
        return impactId;
    }

    public String getFactorName() {
        return factorName;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getActivation() {
        return activation;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getContribution() {
        return contribution;
    }

    public String getReason() {
        return reason;
    }
}
