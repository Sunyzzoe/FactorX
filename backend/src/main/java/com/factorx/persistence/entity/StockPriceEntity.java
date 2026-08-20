package com.factorx.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "stock_prices")
public class StockPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "close_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal closePrice;

    @Column(name = "adjusted_close_price", precision = 20, scale = 8)
    private BigDecimal adjustedClosePrice;

    private Long volume;

    @Column(name = "return_pct", precision = 12, scale = 8)
    private BigDecimal returnPct;

    @Column(length = 64)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockPriceEntity() {
    }

    public StockPriceEntity(
            String symbol,
            LocalDate tradeDate,
            BigDecimal closePrice,
            BigDecimal adjustedClosePrice,
            Long volume,
            BigDecimal returnPct,
            String source
    ) {
        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.closePrice = closePrice;
        this.adjustedClosePrice = adjustedClosePrice;
        this.volume = volume;
        this.returnPct = returnPct;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public StockPriceEntity(
            String symbol, LocalDate tradeDate, BigDecimal closePrice,
            Long volume, BigDecimal returnPct, String source
    ) {
        this(symbol, tradeDate, closePrice, closePrice, volume, returnPct, source);
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public BigDecimal getAdjustedClosePrice() {
        return adjustedClosePrice;
    }

    public Long getVolume() {
        return volume;
    }

    public BigDecimal getReturnPct() {
        return returnPct;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(BigDecimal closePrice, BigDecimal adjustedClosePrice, Long volume,
                       BigDecimal returnPct, String source) {
        this.closePrice = closePrice;
        this.adjustedClosePrice = adjustedClosePrice;
        this.volume = volume;
        this.returnPct = returnPct;
        this.source = source;
    }
}
