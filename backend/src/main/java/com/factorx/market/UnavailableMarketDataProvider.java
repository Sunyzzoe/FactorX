package com.factorx.market;

import com.factorx.market.model.StockPrice;
import com.factorx.market.model.StockQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnMissingBean(MarketDataProvider.class)
public class UnavailableMarketDataProvider implements MarketDataProvider {
    @Override
    public StockQuote getQuote(String symbol) {
        throw new MarketDataException("No market data provider is configured");
    }

    @Override
    public List<StockPrice> getHistory(String symbol, LocalDate startDate, LocalDate endDate) {
        throw new MarketDataException("No market data provider is configured");
    }

    @Override
    public String name() {
        return "unavailable";
    }
}
