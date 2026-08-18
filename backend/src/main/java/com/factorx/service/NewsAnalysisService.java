package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NewsAnalysisService {

    private final EventExtractorService eventExtractorService;
    private final StockMatcherService stockMatcherService;
    private final ScoringService scoringService;
    private final ExplanationService explanationService;

    public NewsAnalysisService(
            EventExtractorService eventExtractorService,
            StockMatcherService stockMatcherService,
            ScoringService scoringService,
            ExplanationService explanationService
    ) {
        this.eventExtractorService = eventExtractorService;
        this.stockMatcherService = stockMatcherService;
        this.scoringService = scoringService;
        this.explanationService = explanationService;
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        AnalysisContext context = new AnalysisContext(request);
        context.event(eventExtractorService.extract(request));
        context.matchedStocks(stockMatcherService.match(context.event(), request));
        context.stockImpacts(scoringService.score(context.event(), context.matchedStocks(), request));
        if (!context.stockImpacts().isEmpty()) {
            context.reluResult(new ReluResult(
                    context.stockImpacts().get(0).reluMomentum(),
                    context.stockImpacts().get(0).reluMetrics(),
                    context.reluFactors(),
                    0
            ));
        }
        context.explanation(explanationService.explain(context));
        context.riskNote(explanationService.riskNote(context));

        return new AnalysisResponse(
                Instant.now().toString(),
                context.event(),
                context.stockImpacts(),
                context.reluMomentum(),
                context.reluMetrics(),
                context.reluFactors(),
                context.explanation(),
                context.riskNote()
        );
    }
}
