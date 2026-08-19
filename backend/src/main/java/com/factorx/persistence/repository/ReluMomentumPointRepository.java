package com.factorx.persistence.repository;

import com.factorx.persistence.entity.ReluMomentumPointEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReluMomentumPointRepository extends JpaRepository<ReluMomentumPointEntity, Long> {

    List<ReluMomentumPointEntity> findByImpactIdOrderByPointIndexAsc(Long impactId);

    Page<ReluMomentumPointEntity> findBySymbolIgnoreCaseOrderByImpactIdDescPointIndexAsc(
            String symbol,
            Pageable pageable
    );
}
