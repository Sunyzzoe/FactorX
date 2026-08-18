package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import com.factorx.model.StockImpact;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringServiceTest {

    private final ScoringService service = new ScoringService(new ReluFactorService());

    @Test
    void returnsTraceableFactorBreakdownForEachStock() {
        StockImpact impact = service.score(
                event(),
                List.of(new MatchedStock("TSLA", "Tesla", "直接相关", 0.82)),
                new AnalysisRequest("Tesla wins a $2B storage contract", "Reuters", "")
        ).get(0);

        assertEquals("利好", impact.direction());
        assertTrue(impact.probability() >= 45 && impact.probability() <= 85);
        assertFalse(impact.factors().isEmpty());
        assertEquals(8, impact.factors().size());
        assertEquals("股票关联度", impact.factors().get(5).name());
        assertEquals(0.64, impact.factors().get(5).activation(), 0.0001);
        assertTrue(impact.factors().stream().anyMatch(factor -> factor.name().equals("事件综合评分") && factor.weight() == 0.30));
        assertFalse(impact.reluMomentum().isEmpty());
        assertEquals("3-10个交易日", impact.horizon());
    }

    @Test
    void doesNotFormatNeutralImpactAsPositiveMove() {
        StockImpact impact = service.score(
                event(),
                List.of(new MatchedStock("TSLA", "Tesla", "直接相关", 0.82)),
                new AnalysisRequest("Tesla update", "Reuters", "Routine operational update")
        ).get(0);

        assertEquals("中性", impact.direction());
        assertEquals("方向不明确", impact.estimatedMove());
    }

    private ExtractedEvent event() {
        return new ExtractedEvent(
                "国际项目",
                "新能源",
                "US",
                2_000_000_000L,
                List.of("Tesla"),
                List.of("storage"),
                "Reuters",
                0.90
        );
    }
}
