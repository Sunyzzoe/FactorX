package com.factorx.persistence.repository;

import com.factorx.persistence.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Page<EventEntity> findByEventTypeIgnoreCaseOrderByCreatedAtDescIdDesc(String eventType, Pageable pageable);

    Page<EventEntity> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
