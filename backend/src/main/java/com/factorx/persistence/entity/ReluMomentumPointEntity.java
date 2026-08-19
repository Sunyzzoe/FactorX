package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "relu_momentum_points")
public class ReluMomentumPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "impact_id", nullable = false)
    private Long impactId;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(name = "point_index", nullable = false)
    private Integer pointIndex;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "return_pct", nullable = false, precision = 12, scale = 8)
    private BigDecimal returnPct;

    @Column(name = "cumulative_return", nullable = false, precision = 12, scale = 8)
    private BigDecimal cumulativeReturn;

    @Column(name = "relu_return", nullable = false, precision = 12, scale = 8)
    private BigDecimal reluReturn;

    @Column(name = "relu_momentum", nullable = false, precision = 12, scale = 8)
    private BigDecimal reluMomentum;

    @Column(nullable = false, precision = 12, scale = 8)
    private BigDecimal threshold;

    protected ReluMomentumPointEntity() {
    }

    public ReluMomentumPointEntity(
            Long impactId,
            String symbol,
            Integer pointIndex,
            BigDecimal price,
            BigDecimal returnPct,
            BigDecimal cumulativeReturn,
            BigDecimal reluReturn,
            BigDecimal reluMomentum,
            BigDecimal threshold
    ) {
        this.impactId = impactId;
        this.symbol = symbol;
        this.pointIndex = pointIndex;
        this.price = price;
        this.returnPct = returnPct;
        this.cumulativeReturn = cumulativeReturn;
        this.reluReturn = reluReturn;
        this.reluMomentum = reluMomentum;
        this.threshold = threshold;
    }

    public Long getId() {
        return id;
    }

    public Long getImpactId() {
        return impactId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Integer getPointIndex() {
        return pointIndex;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getReturnPct() {
        return returnPct;
    }

    public BigDecimal getCumulativeReturn() {
        return cumulativeReturn;
    }

    public BigDecimal getReluReturn() {
        return reluReturn;
    }

    public BigDecimal getReluMomentum() {
        return reluMomentum;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }
}
