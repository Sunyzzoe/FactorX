package com.factorx.backtest;

import com.factorx.persistence.entity.StockPriceEntity;
import com.factorx.persistence.repository.BacktestCandidate;
import com.factorx.persistence.repository.StockImpactRepository;
import com.factorx.persistence.repository.StockPriceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Profile("postgres")
public class BacktestService {
    private final StockImpactRepository impactRepository;
    private final StockPriceRepository priceRepository;

    public BacktestService(StockImpactRepository impactRepository, StockPriceRepository priceRepository) {
        this.impactRepository = impactRepository;
        this.priceRepository = priceRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> backtest(BacktestRequest request) {
        validate(request);
        List<String> symbols = request.symbols() == null ? List.of() : request.symbols().stream()
                .filter(Objects::nonNull).map(s -> s.trim().toUpperCase()).filter(s -> !s.isBlank()).distinct().toList();
        List<BacktestCandidate> candidates = impactRepository.findBacktestCandidates(
                request.start().atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                request.end().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                symbols.isEmpty(), symbols.isEmpty() ? List.of("__ALL__") : symbols);
        List<Sample> samples = candidates.stream().map(c -> sample(c, request.horizons())).filter(Objects::nonNull).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("sampleCount", samples.size());
        result.put("modelVersion", "rule-v1");
        result.put("horizons", request.horizons());
        result.put("byHorizon", metricsByHorizon(samples, request.horizons(), request.cost() + request.slip()));
        result.put("bySector", metricsBySector(samples, request.horizons().get(0), request.cost() + request.slip()));
        result.put("samples", samples.stream().map(Sample::toMap).toList());
        return result;
    }

    private Sample sample(BacktestCandidate candidate, List<Integer> horizons) {
        if (candidate.getEventTime() == null) return null;
        LocalDate eventDate = candidate.getEventTime().atZone(java.time.ZoneOffset.UTC).toLocalDate();
        int maxHorizon = horizons.stream().mapToInt(Integer::intValue).max().orElse(10);
        List<StockPriceEntity> prices = priceRepository.findBySymbolIgnoreCaseAndTradeDateBetweenOrderByTradeDateAsc(
                candidate.getSymbol(), eventDate, eventDate.plusDays(maxHorizon * 3L + 10));
        if (prices.isEmpty()) return null;
        int entryIndex = 0;
        StockPriceEntity entry = prices.get(entryIndex);
        Map<Integer, Double> returns = new HashMap<>();
        for (int horizon : horizons) {
            int targetIndex = entryIndex + horizon;
            if (targetIndex < prices.size()) {
                double entryPrice = value(entry);
                returns.put(horizon, value(prices.get(targetIndex)) / entryPrice - 1.0);
            }
        }
        if (returns.isEmpty()) return null;
        return new Sample(candidate, entry.getTradeDate(), value(entry), returns);
    }

    private Map<String, Object> metricsByHorizon(List<Sample> samples, List<Integer> horizons, double cost) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int horizon : horizons) result.put(String.valueOf(horizon), metrics(samples, horizon, cost));
        return result;
    }

    private Map<String, Object> metricsBySector(List<Sample> samples, int horizon, double cost) {
        Map<String, List<Sample>> grouped = new LinkedHashMap<>();
        samples.forEach(s -> grouped.computeIfAbsent(s.candidate.getSector() == null ? "未分类" : s.candidate.getSector(), k -> new ArrayList<>()).add(s));
        Map<String, Object> result = new LinkedHashMap<>();
        grouped.forEach((sector, values) -> result.put(sector, metrics(values, horizon, cost)));
        return result;
    }

    private Map<String, Object> metrics(List<Sample> samples, int horizon, double cost) {
        List<Sample> usable = samples.stream().filter(s -> s.returns.containsKey(horizon)).toList();
        List<Double> returns = usable.stream().map(s -> signed(s, horizon)).toList();
        long correct = returns.stream().filter(r -> r > 0).count();
        double avg = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double netAvg = avg - cost;
        double winRate = returns.isEmpty() ? 0 : (double) correct / returns.size();
        double avgWin = returns.stream().filter(r -> r > 0).mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = returns.stream().filter(r -> r < 0).mapToDouble(Math::abs).average().orElse(0);
        double equity = 1, peak = 1, maxDrawdown = 0;
        for (double value : returns) { equity *= 1 + value - cost; peak = Math.max(peak, equity); maxDrawdown = Math.min(maxDrawdown, equity / peak - 1); }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sampleCount", usable.size());
        output.put("directionAccuracy", round(winRate));
        output.put("auc", round(auc(usable, horizon)));
        output.put("avgReturn", round(avg));
        output.put("netAvgReturn", round(netAvg));
        output.put("maxDrawdown", round(maxDrawdown));
        output.put("winRate", round(winRate));
        output.put("profitLossRatio", avgLoss == 0 ? 0 : round(avgWin / avgLoss));
        return output;
    }

    private double auc(List<Sample> samples, int horizon) {
        List<Sample> ordered = samples.stream().filter(s -> s.returns.containsKey(horizon))
                .sorted(Comparator.comparingDouble((Sample s) -> s.candidate.getFinalImpactScore().doubleValue())).toList();
        long positives = ordered.stream().filter(s -> signed(s, horizon) > 0).count();
        long negatives = ordered.size() - positives;
        if (positives == 0 || negatives == 0) return 0.5;
        double rankSum = 0; for (int i = 0; i < ordered.size(); i++) if (signed(ordered.get(i), horizon) > 0) rankSum += i + 1;
        return (rankSum - positives * (positives + 1) / 2.0) / (positives * (double) negatives);
    }

    private double signed(Sample sample, int horizon) {
        double raw = sample.returns.get(horizon);
        return "利空".equals(sample.candidate.getDirection()) ? -raw : raw;
    }

    private double value(StockPriceEntity price) { return (price.getAdjustedClosePrice() == null ? price.getClosePrice() : price.getAdjustedClosePrice()).doubleValue(); }
    private double round(double value) { return Math.round(value * 1000000d) / 1000000d; }
    private void validate(BacktestRequest request) {
        if (request.start().isAfter(request.end())) throw new IllegalArgumentException("startDate 不能晚于 endDate");
        if (request.horizons().stream().anyMatch(h -> h == null || h <= 0 || h > 252)) throw new IllegalArgumentException("horizons 必须是 1 到 252 的交易日周期");
        if (request.cost() < 0 || request.slip() < 0) throw new IllegalArgumentException("交易成本和滑点不能为负数");
    }

    public record Sample(BacktestCandidate candidate, LocalDate entryDate, double entryPrice, Map<Integer, Double> returns) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventId", candidate.getEventId()); map.put("impactId", candidate.getImpactId()); map.put("symbol", candidate.getSymbol());
            map.put("eventTime", candidate.getEventTime()); map.put("direction", candidate.getDirection()); map.put("sector", candidate.getSector());
            map.put("finalImpactScore", candidate.getFinalImpactScore()); map.put("relevanceScore", candidate.getRelevanceScore());
            map.put("reluScore", candidate.getReluScore()); map.put("entryDate", entryDate); map.put("entryPrice", entryPrice); map.put("returns", returns);
            return map;
        }
    }
}
