package com.factorx.persistence.repository;

import com.factorx.persistence.entity.NewsArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {

    Optional<NewsArticleEntity> findByContentHash(String contentHash);

    Optional<NewsArticleEntity> findBySourceCodeAndExternalId(String sourceCode, String externalId);

    Optional<NewsArticleEntity> findByUrl(String url);

    Page<NewsArticleEntity> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);

    @Query("""
            select n from NewsArticleEntity n
            where (:sourceCode is null
                    or lower(n.sourceCode) = lower(:sourceCode)
                    or lower(n.source) = lower(:sourceCode))
              and (:from is null or n.publishedAt >= :from)
              and (:to is null or n.publishedAt <= :to)
            order by n.publishedAt desc, n.id desc
            """)
    Page<NewsArticleEntity> search(
            @Param("sourceCode") String sourceCode,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
