package com.factorx.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface BacktestCandidate {
    Long getEventId();
    Long getImpactId();
    String getSymbol();
    String getDirection();
    String getSector();
    Instant getEventTime();
    BigDecimal getFinalImpactScore();
    BigDecimal getRelevanceScore();
    BigDecimal getReluScore();
}
