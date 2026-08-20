package com.factorx.market;

import com.factorx.market.model.MarketSnapshot;

public interface MarketDataService {
    MarketSnapshot analyze(String symbol, String sector, String direction);
}
