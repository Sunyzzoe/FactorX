package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import com.factorx.model.StockImpact;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ScoringService {

    public List<StockImpact> score(
            ExtractedEvent event,
            List<MatchedStock> matchedStocks,
            ReluResult reluResult,
            AnalysisRequest request
    ) {
        String direction = detectDirection(request);
        double eventScore = eventScore(event);
        double sourceScore = event.sourceCredibility();
        double reluScore = reluResult.momentumScore();
        double marketConfirmationScore = 0.42;

        return matchedStocks.stream()
                .map(stock -> toImpact(stock, direction, eventScore, sourceScore, reluScore, marketConfirmationScore))
                .toList();
    }

    private StockImpact toImpact(
            MatchedStock stock,
            String direction,
            double eventScore,
            double sourceScore,
            double reluScore,
            double marketConfirmationScore
    ) {
        double finalImpactScore =
                0.30 * eventScore
                        + 0.25 * stock.relevance()
                        + 0.20 * reluScore
                        + 0.15 * sourceScore
                        + 0.10 * marketConfirmationScore;
        finalImpactScore = clamp(finalImpactScore);

        int probability = (int) Math.round(45 + finalImpactScore * 40);
        double lowMove = round(0.5 + finalImpactScore * 2);
        double highMove = round(lowMove + 1.5 + finalImpactScore * 3);
        String prefix = "利空".equals(direction) ? "-" : "+";

        return new StockImpact(
                stock.symbol(),
                stock.company(),
                stock.relation(),
                direction,
                Math.max(0, Math.min(100, probability)),
                prefix + lowMove + "% ~ " + prefix + highMove + "%",
                "3-10个交易日",
                round(stock.relevance())
        );
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

    private double eventScore(ExtractedEvent event) {
        double projectScaleScore = projectScaleScore(event.projectAmountUsd());
        double companyRelevanceScore = event.companies().contains("待确认") ? 0.45 : 0.80;
        double sectorHeatScore = "待确认".equals(event.sector()) ? 0.50 : 0.72;
        double marketConfirmationScore = 0.42;
        return clamp(
                0.30 * projectScaleScore
                        + 0.20 * event.sourceCredibility()
                        + 0.20 * companyRelevanceScore
                        + 0.15 * sectorHeatScore
                        + 0.15 * marketConfirmationScore
        );
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
