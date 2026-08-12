package com.factorx.api;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.AnalysisResponse;
import com.factorx.model.ExtractedEvent;
import com.factorx.model.ReluFactor;
import com.factorx.model.ReluMomentumPoint;
import com.factorx.model.StockImpact;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnalysisController {

    @GetMapping("/demo")
    public AnalysisResponse demo() {
        return analyze(new AnalysisRequest(
                "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
                "Reuters",
                "The project may accelerate battery storage demand and benefit solar, inverter and AI grid companies."
        ));
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        String text = ((request.headline() == null ? "" : request.headline()) + " "
                + (request.body() == null ? "" : request.body())).toLowerCase(Locale.ROOT);

        String sector = detectSector(text);
        String direction = detectDirection(text);
        double amountScore = hasLargeProject(text) ? 0.88 : 0.55;
        double sourceScore = "reuters".equalsIgnoreCase(request.source()) ? 0.86 : 0.62;
        double relevanceScore = text.contains("tesla") ? 0.91 : 0.68;
        double sectorScore = "新能源".equals(sector) ? 0.78 : 0.65;
        double marketScore = 0.42;
        double reluMomentumScore = 0.74;

        List<ReluFactor> factors = List.of(
                factor("国际项目规模因子", amountScore, 0.35, "项目金额较大，可能带来订单和产业链需求。"),
                factor("新闻源可信度因子", sourceScore, 0.45, "权威媒体来源降低传播噪声。"),
                factor("股票关联度因子", relevanceScore, 0.40, "新闻直接或间接命中公司与产业链关键词。"),
                factor("行业景气因子", sectorScore, 0.38, "新能源、AI 芯片或半导体对国际项目消息较敏感。"),
                factor("市场确认因子", marketScore, 0.55, "第一版尚未接入真实成交量，暂以低确认处理。"),
                factor("ReLU 动量因子", reluMomentumScore, 0.50, "正向收益台阶较连续，平台时间较短。")
        );

        double impactScore = factors.stream()
                .mapToDouble(ReluFactor::activation)
                .average()
                .orElse(0.5);

        int probability = (int) Math.round(45 + impactScore * 40);
        double low = round(0.5 + impactScore * 2);
        double high = round(low + 1.5 + impactScore * 3);
        String movePrefix = "利空".equals(direction) ? "-" : "+";

        List<StockImpact> stocks = List.of(
                new StockImpact("TSLA", "Tesla", "直接相关", direction, probability, movePrefix + low + "% ~ " + movePrefix + high + "%", "3-10个交易日", 0.91),
                new StockImpact("ENPH", "Enphase", "产业链相关", direction, Math.max(probability - 8, 1), movePrefix + "1.1% ~ " + movePrefix + "3.8%", "3-10个交易日", 0.72),
                new StockImpact("NVDA", "Nvidia", "行业相关", direction, Math.max(probability - 13, 1), movePrefix + "0.8% ~ " + movePrefix + "2.9%", "3-10个交易日", 0.61)
        );

        List<ReluMomentumPoint> curve = List.of(
                p(1, 100, 0, 0, 0, 0),
                p(2, 101.2, 1.19, 1.19, 0.79, 0.79),
                p(3, 102.7, 1.47, 2.66, 1.07, 1.86),
                p(4, 101.9, -0.78, 1.88, 0, 1.86),
                p(5, 103.8, 1.85, 3.73, 1.45, 3.31),
                p(6, 104.6, 0.77, 4.50, 0.37, 3.68),
                p(7, 104.1, -0.48, 4.02, 0, 3.68),
                p(8, 104.0, -0.10, 3.92, 0, 3.68),
                p(9, 106.4, 2.28, 6.20, 1.88, 5.56),
                p(10, 108.1, 1.58, 7.78, 1.18, 6.74),
                p(11, 109.9, 1.65, 9.43, 1.25, 7.99),
                p(12, 111.2, 1.18, 10.61, 0.78, 8.77)
        );

        ExtractedEvent event = new ExtractedEvent(
                "国际项目",
                sector,
                detectCountry(text),
                hasLargeProject(text) ? 10_000_000_000L : null,
                List.of("Tesla", "Enphase", "Nvidia"),
                request.source(),
                sourceScore
        );

        return new AnalysisResponse(
                Instant.now().toString(),
                event,
                stocks,
                curve,
                factors,
                "系统识别该新闻属于" + sector + "国际项目事件。项目规模、新闻源可信度和股票关联度因子已激活，"
                        + "TSLA 与储能、电池或相关供应链的关联度最高。ReLU 动量曲线呈现台阶式上升，说明正向收益"
                        + "在阈值截断后仍有累计效应，因此系统判断该事件对 TSLA 短期偏" + direction + "。概率未进一步上调，"
                        + "主要因为市场确认因子尚未接入真实成交量和盘中价格。",
                "市场确认因子尚未完全激活，当前判断主要来自事件文本和产业链匹配。若后续成交量没有放大，"
                        + "或股价已经提前反应，实际影响可能低于模型估计。该结果不构成投资建议。"
        );
    }

    private String detectSector(String text) {
        if (text.contains("solar") || text.contains("battery") || text.contains("storage")) {
            return "新能源";
        }
        if (text.contains("chip") || text.contains("gpu") || text.contains("semiconductor")) {
            return "AI芯片";
        }
        return "综合";
    }

    private String detectDirection(String text) {
        if (text.contains("ban") || text.contains("sanction") || text.contains("delay")) {
            return "利空";
        }
        return "利好";
    }

    private String detectCountry(String text) {
        if (text.contains("saudi")) {
            return "Saudi Arabia";
        }
        return "待确认";
    }

    private boolean hasLargeProject(String text) {
        return text.contains("$10b") || text.contains("10b") || text.contains("10 billion");
    }

    private ReluFactor factor(String name, double raw, double threshold, String reason) {
        double activation = Math.max(0, raw - threshold) / (1 - threshold);
        return new ReluFactor(name, round(raw), threshold, round(activation), reason);
    }

    private ReluMomentumPoint p(int day, double price, double returnPct, double cumulativeReturn, double reluReturn, double reluMomentum) {
        return new ReluMomentumPoint(day, price, returnPct, cumulativeReturn, reluReturn, reluMomentum);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
