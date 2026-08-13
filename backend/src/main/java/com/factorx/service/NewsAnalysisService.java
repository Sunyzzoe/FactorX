package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.AnalysisResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NewsAnalysisService {

    private final EventExtractorService eventExtractorService;
    private final StockMatcherService stockMatcherService;
    private final ReluFactorService reluFactorService;
    private final ScoringService scoringService;
    private final ExplanationService explanationService;

    public NewsAnalysisService(
            EventExtractorService eventExtractorService,
            StockMatcherService stockMatcherService,
            ReluFactorService reluFactorService,
            ScoringService scoringService,
            ExplanationService explanationService
    ) {
        this.eventExtractorService = eventExtractorService;
        this.stockMatcherService = stockMatcherService;
        this.reluFactorService = reluFactorService;
        this.scoringService = scoringService;
        this.explanationService = explanationService;
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        AnalysisContext context = new AnalysisContext(request);
        context.event(eventExtractorService.extract(request));
        context.matchedStocks(stockMatcherService.match(context.event(), request));
        context.reluResult(reluFactorService.calculate(context.matchedStocks()));
        context.stockImpacts(scoringService.score(context.event(), context.matchedStocks(), context.reluResult(), request));
        context.explanation(explanationService.explain(context));
        context.riskNote(explanationService.riskNote(context));

        return new AnalysisResponse(
                Instant.now().toString(),
                context.event(),
                context.stockImpacts(),
                context.reluMomentum(),
                context.reluFactors(),
                context.explanation(),
                context.riskNote()
        );
    }
}
