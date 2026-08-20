package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import com.factorx.model.FactorScore;
import com.factorx.model.StockImpact;
import com.factorx.market.MarketDataService;
import com.factorx.market.model.MarketConfirmation;
import com.factorx.market.model.MarketIndicators;
import com.factorx.market.model.MarketSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

@Service
public class ScoringService {

    private final ReluFactorService reluFactorService;
    private final MarketDataService marketDataService;

    public ScoringService(ReluFactorService reluFactorService) {
        this.reluFactorService = reluFactorService;
        this.marketDataService = (symbol, sector, direction) -> com.factorx.market.model.MarketSnapshot.unavailable();
    }

    @Autowired
    public ScoringService(ReluFactorService reluFactorService, MarketDataService marketDataService) {
        this.reluFactorService = reluFactorService;
        this.marketDataService = marketDataService;
    }

    public List<StockImpact> score(
            ExtractedEvent event,
            List<MatchedStock> matchedStocks,
            AnalysisRequest request
    ) {
        String direction = detectDirection(request);

        return matchedStocks.stream()
                .map(stock -> toImpact(stock, event, direction))
                .toList();
    }

    private StockImpact toImpact(
            MatchedStock stock,
            ExtractedEvent event,
            String direction
    ) {
        MarketSnapshot market = marketDataService.analyze(stock.symbol(), event.sector(), direction);
        ReluResult reluResult = market.reluResult() == null ? reluFactorService.calculate(stock) : market.reluResult();
        double marketScore = market.confirmation() == null ? 0.42 : market.confirmation().score();
        List<FactorScore> factors = factors(event, stock, reluResult, marketScore);
        double eventScore = factor(factors, "事件综合评分").activation();
        double sourceScore = factor(factors, "新闻源可信度").activation();
        double relevanceScore = factor(factors, "股票关联度").activation();
        double reluScore = factor(factors, "ReLU 动量").activation();
        double marketConfirmationScore = factor(factors, "市场确认").activation();
        double finalImpactScore =
                0.30 * eventScore
                        + 0.25 * relevanceScore
                        + 0.20 * reluScore
                        + 0.15 * sourceScore
                        + 0.10 * marketConfirmationScore;
        finalImpactScore = clamp(finalImpactScore);

        int probability = (int) Math.round(45 + finalImpactScore * 40);
        double lowMove = round(0.5 + finalImpactScore * 2);
        double highMove = round(lowMove + 1.5 + finalImpactScore * 3);
        String estimatedMove = estimatedMove(direction, lowMove, highMove);

        return new StockImpact(
                stock.symbol(),
                stock.company(),
                stock.relation(),
                direction,
                Math.max(0, Math.min(100, probability)),
                estimatedMove,
                "3-10个交易日",
                round(stock.relevance()),
                round(finalImpactScore),
                factors,
                reluResult.momentum(),
                reluResult.metrics(),
                market.status(),
                market.indicators(),
                market.confirmation()
        );
    }

    private List<FactorScore> factors(ExtractedEvent event, MatchedStock stock, ReluResult reluResult, double marketConfirmationScore) {
        FactorScore projectScale = factorScore("国际项目规模", projectScaleScore(event.projectAmountUsd()), 0.50, 0,
                "基于事件提取的项目金额，使用对数压缩避免超大金额线性放大。" );
        FactorScore source = factorScore("新闻源可信度", event.sourceCredibility(), 0.60, 0.15,
                "基于新闻源可信度评分。" );
        FactorScore companyClarity = factorScore("公司明确性", companyClarityScore(event), 0.50, 0,
                "事件是否识别出明确的关联公司。" );
        FactorScore industry = factorScore("行业景气", industryScore(event), 0.50, 0,
                "基于行业是否明确；真实行业热度将在数据接入后替换。" );
        FactorScore market = factorScore("市场确认", marketConfirmationScore, 0.50, 0.10,
                marketConfirmationScore == 0.42 ? "当前行情不可用，使用保守的默认确认分。" : "基于价格方向、成交量、行业 ETF 和波动率计算。" );
        FactorScore relevance = factorScore("股票关联度", stock.relevance(), 0.50, 0.25,
                "基于股票匹配模块给出的业务、供应链或行业关联度。" );
        FactorScore relu = factorScore("ReLU 动量", reluResult.momentumScore(), 0.50, 0.20,
                "基于超过收益阈值的正向动量、持续性与平台风险。" );

        double eventRawScore = clamp(
                0.30 * projectScale.activation()
                        + 0.20 * source.activation()
                        + 0.20 * companyClarity.activation()
                        + 0.15 * industry.activation()
                        + 0.15 * market.activation()
        );
        FactorScore eventScore = factorScore("事件综合评分", eventRawScore, 0, 0.30,
                "由项目规模、来源可信度、公司明确性、行业景气和市场确认聚合得出。" );

        return List.of(projectScale, source, companyClarity, industry, market, relevance, relu, eventScore);
    }

    private FactorScore factor(String name, double rawScore, double threshold, double weight, String reason) {
        return factorScore(name, rawScore, threshold, weight, reason);
    }

    private FactorScore factor(List<FactorScore> factors, String name) {
        return factors.stream()
                .filter(factor -> factor.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing factor: " + name));
    }

    private FactorScore factorScore(String name, double rawScore, double threshold, double weight, String reason) {
        double normalizedRawScore = clamp(rawScore);
        double activation = threshold >= 1 ? 0 : clamp(Math.max(0, normalizedRawScore - threshold) / (1 - threshold));
        return new FactorScore(name, round(normalizedRawScore), threshold, round(activation), weight,
                round(activation * weight), reason);
    }

    private String estimatedMove(String direction, double lowMove, double highMove) {
        if ("利好".equals(direction)) {
            return "+" + lowMove + "% ~ +" + highMove + "%";
        }
        if ("利空".equals(direction)) {
            return "-" + lowMove + "% ~ -" + highMove + "%";
        }
        return "方向不明确";
    }

    private String detectDirection(AnalysisRequest request) {
        String text = (request.headline() + " " + nullToEmpty(request.body())).toLowerCase(Locale.ROOT);
        if (containsAny(text, "ban", "sanction", "lawsuit", "delay", "cancel", "probe", "recall", "miss")) {
            return "利空";
        }
        if (containsAny(text, "announce", "wins", "contract", "investment", "subsidy", "partnership", "expansion", "project")) {
            return "利好";
        }
        return "中性";
    }

    private double companyClarityScore(ExtractedEvent event) {
        return event.companies().contains("待确认") ? 0.45 : 0.80;
    }

    private double industryScore(ExtractedEvent event) {
        return "待确认".equals(event.sector()) ? 0.50 : 0.72;
    }

    private double projectScaleScore(Long projectAmountUsd) {
        if (projectAmountUsd == null || projectAmountUsd <= 0) {
            return 0.45;
        }
        double amountBillion = projectAmountUsd / 1_000_000_000.0;
        return clamp(0.25 + Math.log10(1 + amountBillion) * 0.48);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
