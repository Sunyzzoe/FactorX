package com.factorx.service;

import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReluFactorService {

    private static final double THRESHOLD = 0.005;

    public ReluResult calculate(List<MatchedStock> matchedStocks) {
        double[] closePrices = simulatedClosePrices(matchedStocks);
        List<ReluMomentumPoint> points = new ArrayList<>();

        double basePrice = closePrices[0];
        double cumulativeReturn = 0;
        double reluMomentum = 0;
        int positiveDays = 0;
        int plateauDays = 0;

        points.add(new ReluMomentumPoint(-closePrices.length + 1, round(basePrice), 0, 0, 0, 0));

        for (int i = 1; i < closePrices.length; i++) {
            double dailyReturn = Math.log(closePrices[i] / closePrices[i - 1]);
            double reluReturn = Math.max(0, dailyReturn - THRESHOLD);
            cumulativeReturn = Math.log(closePrices[i] / basePrice);
            reluMomentum += reluReturn;

            if (dailyReturn > THRESHOLD) {
                positiveDays++;
            } else {
                plateauDays++;
            }

            points.add(new ReluMomentumPoint(
                    i - closePrices.length + 1,
                    round(closePrices[i]),
                    round(dailyReturn * 100),
                    round(cumulativeReturn * 100),
                    round(reluReturn * 100),
                    round(reluMomentum * 100)
            ));
        }

        int lookbackDays = closePrices.length - 1;
        double reluSlope = reluMomentum / lookbackDays;
        double positiveDensity = (double) positiveDays / lookbackDays;
        double plateauRatio = (double) plateauDays / lookbackDays;
        double momentumPurity = clamp(reluSlope * 100 * positiveDensity * (1 - plateauRatio));
        double momentumScore = clamp(momentumPurity * 1.25 + positiveDensity * 0.25);

        List<ReluFactor> factors = List.of(
                factor("动量纯度", momentumPurity, 0.30, "ReLU 累计动量斜率、正向密度和平台占比共同决定。"),
                factor("正向密度", positiveDensity, 0.45, "超过 0.5% 激活阈值的上涨交易日占比。"),
                factor("平台风险", plateauRatio, 0.50, "未超过激活阈值的交易日占比，数值越高代表信号越弱。"),
                factor("ReLU 动量因子", momentumScore, 0.50, "MVP 使用模拟价格序列，后续可替换为真实收盘价。")
        );

        return new ReluResult(points, factors, momentumScore);
    }

    private double[] simulatedClosePrices(List<MatchedStock> matchedStocks) {
        double relevance = matchedStocks.isEmpty() ? 0.6 : matchedStocks.get(0).relevance();
        double lift = Math.max(0, relevance - 0.5);
        return new double[]{
                100.0,
                100.8 + lift,
                102.0 + lift,
                101.5 + lift,
                103.0 + lift * 1.4,
                104.4 + lift * 1.5,
                104.0 + lift,
                105.2 + lift * 1.6,
                107.3 + lift * 1.8,
                108.4 + lift * 2,
                109.8 + lift * 2.1,
                111.0 + lift * 2.2
        };
    }

    private ReluFactor factor(String name, double rawScore, double threshold, String reason) {
        double activation = Math.max(0, rawScore - threshold) / (1 - threshold);
        return new ReluFactor(name, round(rawScore), threshold, round(clamp(activation)), reason);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
