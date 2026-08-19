package com.factorx.persistence.repository;

import com.factorx.persistence.entity.EventCompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventCompanyRepository extends JpaRepository<EventCompanyEntity, Long> {

    List<EventCompanyEntity> findByEventIdOrderByIdAsc(Long eventId);
}
