package com.factorx.market;

import com.factorx.market.model.StockPrice;
import com.factorx.market.model.StockQuote;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataProvider {
    StockQuote getQuote(String symbol);

    List<StockPrice> getHistory(String symbol, LocalDate startDate, LocalDate endDate);

    String name();
}
