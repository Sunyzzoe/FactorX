package com.factorx.persistence.repository;

import com.factorx.persistence.entity.FactorScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FactorScoreRepository extends JpaRepository<FactorScoreEntity, Long> {

    List<FactorScoreEntity> findByImpactIdOrderByIdAsc(Long impactId);
}
