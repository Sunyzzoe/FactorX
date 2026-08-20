package com.factorx.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Profile("postgres")
@ConditionalOnProperty(prefix = "factorx.market.sync", name = "enabled", havingValue = "true")
public class MarketDataSyncScheduler {
    private final MarketDataService marketDataService;
    private final String symbols;

    public MarketDataSyncScheduler(
            MarketDataService marketDataService,
            @Value("${factorx.market.sync.symbols:}") String symbols
    ) {
        this.marketDataService = marketDataService;
        this.symbols = symbols;
    }

    @Scheduled(cron = "${factorx.market.sync.cron:0 30 22 * * MON-FRI}")
    public void syncDailyMarketData() {
        Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(symbol -> !symbol.isBlank())
                .forEach(symbol -> marketDataService.analyze(symbol, "", "中性"));
    }
}
