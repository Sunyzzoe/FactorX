package com.factorx.service;

import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;
import com.factorx.model.ReluMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ReluFactorService {

    public static final double DEFAULT_THRESHOLD = 0.004;

    public ReluResult calculate(MatchedStock matchedStock) {
        double[] simulatedPrices = simulatedClosePrices(matchedStock);
        List<BigDecimal> closePrices = Arrays.stream(simulatedPrices)
                .mapToObj(BigDecimal::valueOf)
                .toList();
        return calculate(closePrices, BigDecimal.valueOf(DEFAULT_THRESHOLD), closePrices.size() - 1);
    }

    public ReluResult calculate(List<BigDecimal> closePrices, BigDecimal threshold, Integer lookbackDays) {
        validate(closePrices, threshold, lookbackDays);

        int availableReturnDays = closePrices.size() - 1;
        int effectiveLookbackDays = Math.min(lookbackDays, availableReturnDays);
        int startIndex = closePrices.size() - effectiveLookbackDays - 1;
        List<BigDecimal> window = closePrices.subList(startIndex, closePrices.size());

        double thresholdValue = threshold.doubleValue();
        double basePrice = window.get(0).doubleValue();
        double reluMomentum = 0;
        int positiveDays = 0;
        List<ReluMomentumPoint> points = new ArrayList<>();

        points.add(new ReluMomentumPoint(-effectiveLookbackDays, round(basePrice), 0, 0, 0, 0));
        for (int index = 1; index < window.size(); index++) {
            double price = window.get(index).doubleValue();
            double previousPrice = window.get(index - 1).doubleValue();
            double dailyReturn = Math.log(price / previousPrice);
            double reluReturn = Math.max(0, dailyReturn - thresholdValue);
            reluMomentum += reluReturn;

            if (dailyReturn > thresholdValue) {
                positiveDays++;
            }

            points.add(new ReluMomentumPoint(
                    index - effectiveLookbackDays,
                    round(price),
                    round(dailyReturn * 100),
                    round(Math.log(price / basePrice) * 100),
                    round(reluReturn * 100),
                    round(reluMomentum * 100)
            ));
        }

        double reluSlope = reluMomentum / effectiveLookbackDays;
        double positiveDensity = (double) positiveDays / effectiveLookbackDays;
        double plateauRatio = 1 - positiveDensity;
        double momentumPurity = reluSlope * positiveDensity * (1 - plateauRatio);
        ReluMetrics metrics = new ReluMetrics(
                thresholdValue,
                effectiveLookbackDays,
                reluSlope,
                positiveDensity,
                plateauRatio,
                momentumPurity
        );

        double momentumScore = clamp(momentumPurity * 100 * 1.25 + positiveDensity * 0.25);
        List<ReluFactor> factors = List.of(
                factor("Alpha Purity", momentumPurity, 0.003, "Effective positive momentum after threshold filtering."),
                factor("Positive Momentum Density", positiveDensity, 0.45, "Share of return days above the ReLU threshold."),
                factor("Plateau Risk", 1 - plateauRatio, 0.50, "Higher activation means less time spent on the ReLU plateau."),
                factor("ReLU Momentum", momentumScore, 0.50, "Normalized confirmation score for the impact model.")
        );

        return new ReluResult(points, metrics, factors, momentumScore);
    }

    private void validate(List<BigDecimal> closePrices, BigDecimal threshold, Integer lookbackDays) {
        if (closePrices == null || closePrices.size() < 2) {
            throw new IllegalArgumentException("closePrices must contain at least two positive prices.");
        }
        if (closePrices.stream().anyMatch(price -> price == null || price.signum() <= 0)) {
            throw new IllegalArgumentException("closePrices must contain only positive values.");
        }
        if (threshold == null || threshold.signum() < 0) {
            throw new IllegalArgumentException("threshold must be zero or positive.");
        }
        if (lookbackDays == null || lookbackDays < 1) {
            throw new IllegalArgumentException("lookbackDays must be at least one.");
        }
    }

    private double[] simulatedClosePrices(MatchedStock matchedStock) {
        double relevance = matchedStock == null ? 0.6 : matchedStock.relevance();
        double lift = Math.max(0, relevance - 0.5);
        return new double[]{
                100.0, 100.8 + lift, 102.0 + lift, 101.5 + lift,
                103.0 + lift * 1.4, 104.4 + lift * 1.5, 104.0 + lift,
                105.2 + lift * 1.6, 107.3 + lift * 1.8, 108.4 + lift * 2,
                109.8 + lift * 2.1, 111.0 + lift * 2.2
        };
    }

    private ReluFactor factor(String name, double rawScore, double threshold, String reason) {
        double activation = threshold >= 1 ? 0 : Math.max(0, rawScore - threshold) / (1 - threshold);
        return new ReluFactor(name, round(rawScore), threshold, round(clamp(activation)), reason);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
