package com.factorx.persistence.repository;

import com.factorx.persistence.entity.StockImpactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StockImpactRepository extends JpaRepository<StockImpactEntity, Long> {

    Page<StockImpactEntity> findBySymbolIgnoreCaseOrderByIdDesc(String symbol, Pageable pageable);

    List<StockImpactEntity> findByEventIdOrderByIdAsc(Long eventId);

    @Query("""
            select i.id as impactId, i.eventId as eventId, i.symbol as symbol,
                   i.direction as direction, e.sector as sector,
                   n.publishedAt as eventTime, i.finalImpactScore as finalImpactScore,
                   i.relevanceScore as relevanceScore,
                   coalesce((select f.activation from FactorScoreEntity f
                             where f.impactId = i.id and f.factorName = 'ReLU 动量'), 0)
                   as reluScore
            from StockImpactEntity i
            join EventEntity e on e.id = i.eventId
            join AnalysisRunEntity r on r.id = e.analysisId
            join NewsArticleEntity n on n.id = r.newsId
            where n.publishedAt >= :from and n.publishedAt <= :to
              and (:symbolsEmpty = true or upper(i.symbol) in :symbols)
            order by n.publishedAt asc, i.id asc
            """)
    List<BacktestCandidate> findBacktestCandidates(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("symbolsEmpty") boolean symbolsEmpty,
            @Param("symbols") List<String> symbols
    );
}
