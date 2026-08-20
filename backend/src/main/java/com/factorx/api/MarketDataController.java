package com.factorx.api;

import com.factorx.market.MarketDataProvider;
import com.factorx.market.model.StockPrice;
import com.factorx.market.model.StockQuote;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class MarketDataController {
    private final ObjectProvider<MarketDataProvider> provider;

    public MarketDataController(ObjectProvider<MarketDataProvider> provider) {
        this.provider = provider;
    }

    @GetMapping("/{symbol}/quote")
    public StockQuote quote(@PathVariable String symbol) {
        return provider.getObject().getQuote(symbol.toUpperCase());
    }

    @GetMapping("/{symbol}/history")
    public List<StockPrice> history(
            @PathVariable String symbol,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(180) : from;
        return provider.getObject().getHistory(symbol.toUpperCase(), start, end);
    }
}
