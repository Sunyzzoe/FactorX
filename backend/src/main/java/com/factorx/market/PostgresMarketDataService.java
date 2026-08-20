package com.factorx.market;

import com.factorx.market.model.IndustryEtfMapping;
import com.factorx.market.model.MarketConfirmation;
import com.factorx.market.model.MarketIndicators;
import com.factorx.market.model.MarketSnapshot;
import com.factorx.market.model.StockPrice;
import com.factorx.market.model.StockQuote;
import com.factorx.persistence.entity.StockPriceEntity;
import com.factorx.persistence.repository.StockPriceRepository;
import com.factorx.service.ReluFactorService;
import com.factorx.service.ReluResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("postgres")
public class PostgresMarketDataService implements MarketDataService {
    private static final Map<String, IndustryEtfMapping> ETF_BY_SECTOR = Map.of(
            "新能源", new IndustryEtfMapping("ICLN", 1),
            "半导体", new IndustryEtfMapping("SOXX", 1),
            "AI 芯片", new IndustryEtfMapping("QQQ", 1),
            "金融", new IndustryEtfMapping("XLF", 1),
            "能源", new IndustryEtfMapping("XLE", 1),
            "生物医药", new IndustryEtfMapping("XBI", 1)
    );

    private final MarketDataProvider provider;
    private final StockPriceRepository stockPriceRepository;
    private final ReluFactorService reluFactorService;
    private final int historyDays;
    private final BigDecimal threshold;

    public PostgresMarketDataService(
            MarketDataProvider provider,
            StockPriceRepository stockPriceRepository,
            ReluFactorService reluFactorService,
            @Value("${factorx.market.history-days:120}") int historyDays,
            @Value("${factorx.market.relu-threshold:0.004}") BigDecimal threshold
    ) {
        this.provider = provider;
        this.stockPriceRepository = stockPriceRepository;
        this.reluFactorService = reluFactorService;
        this.historyDays = Math.max(60, historyDays);
        this.threshold = threshold;
    }

    @Override
    @Transactional
    public MarketSnapshot analyze(String symbol, String sector, String direction) {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays((long) Math.ceil(historyDays * 1.7));
            List<StockPrice> fetched = clean(provider.getHistory(symbol, start, end));
            if (fetched.size() < 21) {
                return new MarketSnapshot("INSUFFICIENT_DATA", null, null, null, provider.name());
            }
            persist(fetched);
            List<StockPrice> prices = fetched.size() > historyDays
                    ? fetched.subList(fetched.size() - historyDays, fetched.size()) : fetched;
            ReluResult relu = reluFactorService.calculate(
                    prices.stream().map(StockPrice::adjustedClosePrice).toList(), threshold, prices.size() - 1);
            MarketIndicators indicators = indicators(prices);
            MarketConfirmation confirmation = confirmation(symbol, sector, direction, indicators);
            return new MarketSnapshot("AVAILABLE", relu, indicators, confirmation, provider.name());
        } catch (MarketDataException | IllegalArgumentException ex) {
            return new MarketSnapshot("UNAVAILABLE", null, null, null, provider.name());
        }
    }

    private MarketIndicators indicators(List<StockPrice> prices) {
        int last = prices.size() - 1;
        double return1d = logReturn(prices.get(last), prices.get(last - 1));
        double return5d = last >= 5 ? Math.log(value(prices.get(last)) / value(prices.get(last - 5))) : return1d;
        double currentVolume = safeVolume(prices.get(last));
        int from = Math.max(0, last - 20);
        double averageVolume = prices.subList(from, last).stream().mapToDouble(this::safeVolume).average().orElse(0);
        double volumeRatio = averageVolume <= 0 ? 0 : currentVolume / averageVolume;
        double[] returns = new double[Math.max(1, last - from)];
        int index = 0;
        for (int i = from + 1; i <= last && index < returns.length; i++) {
            returns[index++] = logReturn(prices.get(i), prices.get(i - 1));
        }
        double dailyVolatility = standardDeviation(returns, index);
        return new MarketIndicators(round(return1d), round(return5d), currentVolume, round(averageVolume),
                round(volumeRatio), round(dailyVolatility), round(dailyVolatility * Math.sqrt(252)));
    }

    private MarketConfirmation confirmation(String symbol, String sector, String direction, MarketIndicators indicators) {
        IndustryEtfMapping mapping = ETF_BY_SECTOR.get(sector);
        Double etfReturn = null;
        if (mapping != null) {
            try {
                LocalDate end = LocalDate.now();
                List<StockPrice> etf = clean(provider.getHistory(mapping.symbol(), end.minusDays(10), end));
                if (etf.size() >= 2) etfReturn = logReturn(etf.get(etf.size() - 1), etf.get(etf.size() - 2));
            } catch (MarketDataException ignored) {
                // ETF data is supplemental; the stock snapshot remains usable.
            }
        }
        boolean positive = "利好".equals(direction);
        boolean priceConfirmed = positive ? indicators.return1d() > 0 : "利空".equals(direction) && indicators.return1d() < 0;
        boolean conflict = (positive && indicators.return1d() < 0) || ("利空".equals(direction) && indicators.return1d() > 0);
        boolean sectorConfirmed = etfReturn == null ? false : positive ? etfReturn > 0 : "利空".equals(direction) && etfReturn < 0;
        double volumeConfirmation = Math.min(indicators.volumeRatio() / 2.0, 1);
        double priceScore = "中性".equals(direction) ? 0.5 : priceConfirmed ? 1 : 0;
        double sectorScore = etfReturn == null ? 0.5 : sectorConfirmed ? 1 : 0;
        double volatilityAdjustment = indicators.annualizedVolatility() != null && indicators.annualizedVolatility() > 0.80 ? 0.25 : 0.75;
        double score = clamp(0.40 * volumeConfirmation + 0.30 * priceScore + 0.20 * sectorScore + 0.10 * volatilityAdjustment);
        String risk = conflict ? "新闻方向与股票短期价格反向，市场尚未确认该事件。" : null;
        return new MarketConfirmation(round(score), priceConfirmed, indicators.volumeRatio() >= 2,
                sectorConfirmed, conflict, mapping == null ? null : mapping.symbol(), etfReturn, risk);
    }

    private void persist(List<StockPrice> prices) {
        for (StockPrice price : prices) {
            StockPriceEntity entity = stockPriceRepository.findBySymbolAndTradeDate(price.symbol(), price.tradeDate())
                    .orElseGet(() -> new StockPriceEntity(price.symbol(), price.tradeDate(), price.closePrice(),
                            price.adjustedClosePrice(), price.volume(), price.returnPct(), price.provider()));
            entity.update(price.closePrice(), price.adjustedClosePrice(), price.volume(), price.returnPct(), price.provider());
            stockPriceRepository.save(entity);
        }
    }

    private List<StockPrice> clean(List<StockPrice> prices) {
        List<StockPrice> sorted = prices == null ? List.of() : prices.stream()
                .filter(p -> p != null && p.tradeDate() != null && p.closePrice() != null && p.closePrice().signum() > 0)
                .sorted(Comparator.comparing(StockPrice::tradeDate))
                .toList();
        java.util.ArrayList<StockPrice> normalized = new java.util.ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            StockPrice current = sorted.get(i);
            BigDecimal returnPct = i == 0 ? null : BigDecimal.valueOf(logReturn(current, sorted.get(i - 1)))
                    .setScale(8, RoundingMode.HALF_UP);
            normalized.add(new StockPrice(current.symbol(), current.tradeDate(), current.closePrice(),
                    current.adjustedClosePrice(), current.volume(), returnPct, current.currency(), current.provider()));
        }
        return normalized;
    }

    private double logReturn(StockPrice current, StockPrice previous) {
        return Math.log(value(current) / value(previous));
    }

    private double value(StockPrice price) {
        BigDecimal adjusted = price.adjustedClosePrice() == null ? price.closePrice() : price.adjustedClosePrice();
        return adjusted.doubleValue();
    }

    private double safeVolume(StockPrice price) {
        return price.volume() == null ? 0 : Math.max(0, price.volume());
    }

    private double standardDeviation(double[] values, int length) {
        if (length < 2) return 0;
        double mean = 0;
        for (int i = 0; i < length; i++) mean += values[i];
        mean /= length;
        double sum = 0;
        for (int i = 0; i < length; i++) sum += Math.pow(values[i] - mean, 2);
        return Math.sqrt(sum / (length - 1));
    }

    private double clamp(double value) { return Math.max(0, Math.min(1, value)); }

    private double round(double value) { return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue(); }
}
