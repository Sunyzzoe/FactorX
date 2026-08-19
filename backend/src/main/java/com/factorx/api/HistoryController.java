package com.factorx.api;

import com.factorx.persistence.entity.EventEntity;
import com.factorx.persistence.entity.NewsArticleEntity;
import com.factorx.persistence.entity.ReluMomentumPointEntity;
import com.factorx.persistence.entity.StockImpactEntity;
import com.factorx.persistence.repository.EventRepository;
import com.factorx.persistence.repository.NewsArticleRepository;
import com.factorx.persistence.repository.ReluMomentumPointRepository;
import com.factorx.persistence.repository.StockImpactRepository;
import com.factorx.news.NewsIngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@Profile("postgres")
public class HistoryController {

    private final NewsArticleRepository newsArticleRepository;
    private final EventRepository eventRepository;
    private final StockImpactRepository stockImpactRepository;
    private final ReluMomentumPointRepository reluMomentumPointRepository;
    private final NewsIngestionService newsIngestionService;

    public HistoryController(
            NewsArticleRepository newsArticleRepository,
            EventRepository eventRepository,
            StockImpactRepository stockImpactRepository,
            ReluMomentumPointRepository reluMomentumPointRepository,
            NewsIngestionService newsIngestionService
    ) {
        this.newsArticleRepository = newsArticleRepository;
        this.eventRepository = eventRepository;
        this.stockImpactRepository = stockImpactRepository;
        this.reluMomentumPointRepository = reluMomentumPointRepository;
        this.newsIngestionService = newsIngestionService;
    }

    @GetMapping("/news")
    public PageResponse<NewsItem> news(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        Page<NewsArticleEntity> result = newsArticleRepository
                .search(
                        StringUtils.hasText(source) ? source.trim() : null,
                        parseInstant(from),
                        parseInstant(to),
                        pageable(page, size)
                );
        return PageResponse.from(result.map(NewsItem::from));
    }

    @GetMapping("/news/{id}")
    public ResponseEntity<NewsItem> newsDetail(@PathVariable Long id) {
        return newsArticleRepository.findById(id)
                .map(entity -> ResponseEntity.ok(NewsItem.from(entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/news/{id}/reanalyze")
    public ResponseEntity<NewsItem> reanalyze(@PathVariable Long id) {
        return newsArticleRepository.findById(id)
                .map(entity -> {
                    newsIngestionService.analyzeAsync(entity.getId());
                    return ResponseEntity.accepted().body(NewsItem.from(entity));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/events")
    public PageResponse<EventItem> events(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = pageable(page, size);
        Page<EventEntity> result = StringUtils.hasText(eventType)
                ? eventRepository.findByEventTypeIgnoreCaseOrderByCreatedAtDescIdDesc(eventType.trim(), pageable)
                : eventRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        return PageResponse.from(result.map(EventItem::from));
    }

    @GetMapping("/stocks/{symbol}/impacts")
    public PageResponse<StockImpactItem> impacts(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<StockImpactEntity> result = stockImpactRepository
                .findBySymbolIgnoreCaseOrderByIdDesc(symbol.trim(), pageable(page, size));
        return PageResponse.from(result.map(StockImpactItem::from));
    }

    @GetMapping("/stocks/{symbol}/relu-momentum")
    public PageResponse<ReluMomentumPointItem> reluMomentum(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        Page<ReluMomentumPointEntity> result = reluMomentumPointRepository
                .findBySymbolIgnoreCaseOrderByImpactIdDescPointIndexAsc(
                        symbol.trim(),
                        pageable(page, size)
                );
        return PageResponse.from(result.map(ReluMomentumPointItem::from));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 1000);
        return PageRequest.of(safePage, safeSize, Sort.unsorted());
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }

    public record NewsItem(
            Long id,
            String title,
            String source,
            String url,
            String body,
            String language,
            Instant publishedAt,
            Instant fetchedAt,
            String hash,
            Instant createdAt,
            String sourceCode,
            String region,
            String sectorHint,
            String status,
            int retryCount,
            String lastError
    ) {
        static NewsItem from(NewsArticleEntity entity) {
            return new NewsItem(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getSource(),
                    entity.getUrl(),
                    entity.getBody(),
                    entity.getLanguage(),
                    entity.getPublishedAt(),
                    entity.getFetchedAt(),
                    entity.getContentHash(),
                    entity.getCreatedAt(),
                    entity.getSourceCode(),
                    entity.getRegion(),
                    entity.getSectorHint(),
                    entity.getStatus(),
                    entity.getRetryCount(),
                    entity.getLastError()
            );
        }
    }

    public record EventItem(
            Long id,
            Long analysisId,
            String eventType,
            String sector,
            String country,
            BigDecimal projectAmountUsd,
            BigDecimal sourceCredibility,
            String summary,
            Instant createdAt
    ) {
        static EventItem from(EventEntity entity) {
            return new EventItem(
                    entity.getId(),
                    entity.getAnalysisId(),
                    entity.getEventType(),
                    entity.getSector(),
                    entity.getCountry(),
                    entity.getProjectAmountUsd(),
                    entity.getSourceCredibility(),
                    entity.getSummary(),
                    entity.getCreatedAt()
            );
        }
    }

    public record StockImpactItem(
            Long id,
            Long eventId,
            String symbol,
            String companyName,
            String direction,
            BigDecimal probability,
            BigDecimal estimatedLow,
            BigDecimal estimatedHigh,
            String horizon,
            BigDecimal relevanceScore,
            BigDecimal finalImpactScore,
            String explanation,
            String riskNote,
            Instant createdAt
    ) {
        static StockImpactItem from(StockImpactEntity entity) {
            return new StockImpactItem(
                    entity.getId(),
                    entity.getEventId(),
                    entity.getSymbol(),
                    entity.getCompanyName(),
                    entity.getDirection(),
                    entity.getProbability(),
                    entity.getEstimatedLow(),
                    entity.getEstimatedHigh(),
                    entity.getHorizon(),
                    entity.getRelevanceScore(),
                    entity.getFinalImpactScore(),
                    entity.getExplanation(),
                    entity.getRiskNote(),
                    entity.getCreatedAt()
            );
        }
    }

    public record ReluMomentumPointItem(
            Long id,
            Long impactId,
            String symbol,
            Integer pointIndex,
            LocalDate tradeDate,
            BigDecimal price,
            BigDecimal returnPct,
            BigDecimal cumulativeReturn,
            BigDecimal reluReturn,
            BigDecimal reluMomentum,
            BigDecimal threshold
    ) {
        static ReluMomentumPointItem from(ReluMomentumPointEntity entity) {
            return new ReluMomentumPointItem(
                    entity.getId(),
                    entity.getImpactId(),
                    entity.getSymbol(),
                    entity.getPointIndex(),
                    entity.getTradeDate(),
                    entity.getPrice(),
                    entity.getReturnPct(),
                    entity.getCumulativeReturn(),
                    entity.getReluReturn(),
                    entity.getReluMomentum(),
                    entity.getThreshold()
            );
        }
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("from/to 必须是 ISO-8601 时间，例如 2026-08-19T00:00:00Z");
        }
    }
}
