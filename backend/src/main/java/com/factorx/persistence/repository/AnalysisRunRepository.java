package com.factorx.persistence.repository;

import com.factorx.persistence.entity.AnalysisRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRunEntity, Long> {
}
