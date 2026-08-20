package com.factorx.persistence.repository;

import com.factorx.persistence.entity.StockPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPriceEntity, Long> {
    Optional<StockPriceEntity> findBySymbolAndTradeDate(String symbol, LocalDate tradeDate);

    List<StockPriceEntity> findTop120BySymbolOrderByTradeDateDesc(String symbol);

    List<StockPriceEntity> findBySymbolIgnoreCaseAndTradeDateBetweenOrderByTradeDateAsc(
            String symbol, LocalDate from, LocalDate to);
}
