package com.factorx.persistence;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.FactorScore;
import com.factorx.model.ReluMomentumPoint;
import com.factorx.model.ReluMetrics;
import com.factorx.model.StockImpact;
import com.factorx.persistence.entity.AnalysisRunEntity;
import com.factorx.persistence.entity.EventCompanyEntity;
import com.factorx.persistence.entity.EventEntity;
import com.factorx.persistence.entity.FactorScoreEntity;
import com.factorx.persistence.entity.NewsArticleEntity;
import com.factorx.persistence.entity.ReluMomentumPointEntity;
import com.factorx.persistence.entity.StockImpactEntity;
import com.factorx.persistence.repository.AnalysisRunRepository;
import com.factorx.persistence.repository.EventCompanyRepository;
import com.factorx.persistence.repository.EventRepository;
import com.factorx.persistence.repository.FactorScoreRepository;
import com.factorx.persistence.repository.NewsArticleRepository;
import com.factorx.persistence.repository.ReluMomentumPointRepository;
import com.factorx.persistence.repository.StockImpactRepository;
import com.factorx.service.AnalysisContext;
import com.factorx.service.MatchedStock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("postgres")
public class AnalysisPersistenceService {

    private final NewsArticleRepository newsArticleRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EventRepository eventRepository;
    private final EventCompanyRepository eventCompanyRepository;
    private final StockImpactRepository stockImpactRepository;
    private final FactorScoreRepository factorScoreRepository;
    private final ReluMomentumPointRepository reluMomentumPointRepository;
    private final NewsContentHasher newsContentHasher;
    private final String modelVersion;

    public AnalysisPersistenceService(
            NewsArticleRepository newsArticleRepository,
            AnalysisRunRepository analysisRunRepository,
            EventRepository eventRepository,
            EventCompanyRepository eventCompanyRepository,
            StockImpactRepository stockImpactRepository,
            FactorScoreRepository factorScoreRepository,
            ReluMomentumPointRepository reluMomentumPointRepository,
            NewsContentHasher newsContentHasher,
            @Value("${FACTORX_MODEL_VERSION:rule-v1}") String modelVersion
    ) {
        this.newsArticleRepository = newsArticleRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.eventRepository = eventRepository;
        this.eventCompanyRepository = eventCompanyRepository;
        this.stockImpactRepository = stockImpactRepository;
        this.factorScoreRepository = factorScoreRepository;
        this.reluMomentumPointRepository = reluMomentumPointRepository;
        this.newsContentHasher = newsContentHasher;
        this.modelVersion = modelVersion;
    }

    @Transactional
    public Long persist(AnalysisContext context) {
        AnalysisRequest request = context.request();
        String contentHash = newsContentHasher.hash(request);
        NewsArticleEntity news = newsArticleRepository.findByContentHash(contentHash)
                .orElseGet(() -> newsArticleRepository.save(new NewsArticleEntity(
                        request.headline(),
                        request.source(),
                        request.body(),
                        contentHash,
                        java.time.Instant.now()
                )));

        AnalysisRunEntity run = analysisRunRepository.save(new AnalysisRunEntity(
                news.getId(),
                modelVersion,
                parameters(context)
        ));

        EventEntity event = eventRepository.save(new EventEntity(
                run.getId(),
                context.event().eventType(),
                context.event().sector(),
                context.event().country(),
                decimal(context.event().projectAmountUsd()),
                decimal(context.event().sourceCredibility())
        ));

        for (MatchedStock stock : context.matchedStocks()) {
            eventCompanyRepository.save(new EventCompanyEntity(
                    event.getId(),
                    stock.company(),
                    stock.symbol(),
                    stock.relation(),
                    decimal(stock.relevance())
            ));
        }

        for (StockImpact impact : context.stockImpacts()) {
            StockImpactEntity impactEntity = stockImpactRepository.save(new StockImpactEntity(
                    event.getId(),
                    impact.symbol(),
                    impact.company(),
                    impact.direction(),
                    decimal(impact.probability()),
                    estimatedMovePart(impact.estimatedMove(), 0),
                    estimatedMovePart(impact.estimatedMove(), 1),
                    impact.horizon(),
                    decimal(impact.relevance()),
                    decimal(impact.finalImpactScore()),
                    context.explanation(),
                    context.riskNote()
            ));
            saveFactors(impactEntity.getId(), impact.factors());
            saveMomentumPoints(impactEntity.getId(), impact);
        }

        run.markSucceeded();
        return run.getId();
    }

    private Map<String, Object> parameters(AnalysisContext context) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        ReluMetrics metrics = context.reluMetrics();
        if (metrics != null) {
            parameters.put("threshold", metrics.threshold());
            parameters.put("lookbackDays", metrics.lookbackDays());
        }
        parameters.put("modelVersion", modelVersion);
        return parameters;
    }

    private void saveFactors(Long impactId, List<FactorScore> factors) {
        for (FactorScore factor : factors) {
            factorScoreRepository.save(new FactorScoreEntity(
                    impactId,
                    factor.name(),
                    decimal(factor.rawScore()),
                    decimal(factor.threshold()),
                    decimal(factor.activation()),
                    decimal(factor.weight()),
                    decimal(factor.contribution()),
                    factor.reason()
            ));
        }
    }

    private void saveMomentumPoints(Long impactId, StockImpact impact) {
        double threshold = impact.reluMetrics() == null ? 0 : impact.reluMetrics().threshold();
        List<ReluMomentumPoint> points = impact.reluMomentum();
        for (int index = 0; index < points.size(); index++) {
            ReluMomentumPoint point = points.get(index);
            reluMomentumPointRepository.save(new ReluMomentumPointEntity(
                    impactId,
                    impact.symbol(),
                    index,
                    decimal(point.price()),
                    decimal(point.returnPct()),
                    decimal(point.cumulativeReturn()),
                    decimal(point.reluReturn()),
                    decimal(point.reluMomentum()),
                    decimal(threshold)
            ));
        }
    }

    private BigDecimal estimatedMovePart(String estimatedMove, int index) {
        if (estimatedMove == null || estimatedMove.isBlank() || "方向不明确".equals(estimatedMove)) {
            return null;
        }
        String[] parts = estimatedMove
                .replace("+", "")
                .replace("%", "")
                .split("~");
        if (parts.length != 2) {
            return null;
        }
        String value = parts[index].trim().replace("-", "");
        return new BigDecimal(value);
    }

    private BigDecimal decimal(Number value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }
}
