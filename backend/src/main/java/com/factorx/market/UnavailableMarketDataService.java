package com.factorx.market;

import com.factorx.market.model.MarketSnapshot;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!postgres")
public class UnavailableMarketDataService implements MarketDataService {
    @Override
    public MarketSnapshot analyze(String symbol, String sector, String direction) {
        return MarketSnapshot.unavailable();
    }
}
