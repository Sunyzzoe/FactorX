package com.factorx.persistence.repository;

import com.factorx.persistence.entity.StockImpactEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockImpactRepository extends JpaRepository<StockImpactEntity, Long> {

    Page<StockImpactEntity> findBySymbolIgnoreCaseOrderByIdDesc(String symbol, Pageable pageable);

    List<StockImpactEntity> findByEventIdOrderByIdAsc(Long eventId);
}
