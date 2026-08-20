package com.factorx.backtest;

import com.factorx.persistence.entity.StockPriceEntity;
import com.factorx.persistence.repository.BacktestCandidate;
import com.factorx.persistence.repository.StockImpactRepository;
import com.factorx.persistence.repository.StockPriceRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BacktestServiceTest {
    @Test
    void calculatesSignedReturnsAndMetricsForBearishEvent() {
        StockImpactRepository impacts = mock(StockImpactRepository.class);
        StockPriceRepository prices = mock(StockPriceRepository.class);
        BacktestCandidate candidate = candidate("利空", "半导体");
        when(impacts.findBacktestCandidates(any(), any(), anyBoolean(), anyList())).thenReturn(List.of(candidate));
        when(prices.findBySymbolIgnoreCaseAndTradeDateBetweenOrderByTradeDateAsc(any(), any(), any()))
                .thenReturn(List.of(
                        price("NVDA", "2024-01-02", "100"),
                        price("NVDA", "2024-01-03", "98"),
                        price("NVDA", "2024-01-04", "95")
                ));

        Map<String, Object> result = new BacktestService(impacts, prices).backtest(
                new BacktestRequest("2024-01-01", "2024-01-05", List.of("NVDA"), List.of(1, 2), 0d, 0d));

        Map<?, ?> dayOne = (Map<?, ?>) ((Map<?, ?>) result.get("byHorizon")).get("1");
        assertThat(dayOne.get("sampleCount")).isEqualTo(1);
        assertThat(dayOne.get("directionAccuracy")).isEqualTo(1.0);
        assertThat(dayOne.get("avgReturn")).isEqualTo(0.02);
    }

    private BacktestCandidate candidate(String direction, String sector) {
        BacktestCandidate candidate = mock(BacktestCandidate.class);
        when(candidate.getEventId()).thenReturn(1L); when(candidate.getImpactId()).thenReturn(2L);
        when(candidate.getSymbol()).thenReturn("NVDA"); when(candidate.getDirection()).thenReturn(direction);
        when(candidate.getSector()).thenReturn(sector); when(candidate.getEventTime()).thenReturn(Instant.parse("2024-01-01T12:00:00Z"));
        when(candidate.getFinalImpactScore()).thenReturn(new BigDecimal("0.8"));
        when(candidate.getRelevanceScore()).thenReturn(new BigDecimal("0.7")); when(candidate.getReluScore()).thenReturn(new BigDecimal("0.6"));
        return candidate;
    }

    private StockPriceEntity price(String symbol, String date, String close) {
        return new StockPriceEntity(symbol, LocalDate.parse(date), new BigDecimal(close), new BigDecimal(close), 100L, null, "test");
    }
}
