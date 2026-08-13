package com.factorx.service;

import com.factorx.model.StockImpact;
import org.springframework.stereotype.Service;

@Service
public class ExplanationService {

    public String explain(AnalysisContext context) {
        StockImpact lead = context.stockImpacts().isEmpty() ? null : context.stockImpacts().get(0);
        String leadStock = lead == null ? "相关股票" : lead.symbol();
        String direction = lead == null ? "中性" : lead.direction();

        return "该新闻被识别为" + context.event().sector() + "领域的" + context.event().eventType()
                + "。事件金额、新闻源可信度、公司/行业匹配关系和 ReLU 动量因子已进入综合评分。"
                + leadStock + " 的关联度最高，当前模型判断短期影响方向为" + direction
                + "。ReLU 曲线使用 0.5% 阈值截断低效收益，仅累计超过阈值的正向动量，"
                + "因此可以区分普通波动和更有意义的正向动量结构。";
    }

    public String riskNote(AnalysisContext context) {
        return "MVP 阶段暂未接入真实成交量、实时行情、数据库和外部 AI，市场确认因子保持较低权重。"
                + "当前结果主要用于前后端联调和分析链路验证，不构成投资建议；后续应结合官方公告、订单落地进度和真实价格数据复核。";
    }
}
