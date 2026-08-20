import type { AnalysisRequest, AnalysisResponse } from "../types/analysis";

const demoReluMomentum = [
  { day: 1, price: 100, returnPct: 0, cumulativeReturn: 0, reluReturn: 0, reluMomentum: 0 },
  { day: 2, price: 101.2, returnPct: 1.19, cumulativeReturn: 1.19, reluReturn: 0.79, reluMomentum: 0.79 }
];

const demoReluMetrics = {
  threshold: 0.004,
  lookbackDays: 11,
  reluSlope: 0.00797,
  positiveDensity: 0.73,
  plateauRatio: 0.27,
  momentumPurity: 0.00425
};

const demoFactors = [
  { name: "国际项目规模", rawScore: 0.88, threshold: 0.5, activation: 0.76, weight: 0, contribution: 0, reason: "项目金额较大，采用对数压缩。" },
  { name: "新闻源可信度", rawScore: 0.86, threshold: 0.6, activation: 0.65, weight: 0.15, contribution: 0.1, reason: "权威媒体来源降低传播噪声。" },
  { name: "股票关联度", rawScore: 0.91, threshold: 0.5, activation: 0.82, weight: 0.25, contribution: 0.21, reason: "公司与事件高度匹配。" },
  { name: "行业景气", rawScore: 0.78, threshold: 0.5, activation: 0.56, weight: 0, contribution: 0, reason: "行业明确且受事件影响。" },
  { name: "市场确认", rawScore: 0.42, threshold: 0.5, activation: 0, weight: 0.1, contribution: 0, reason: "尚未接入真实量价数据。" },
  { name: "ReLU 动量", rawScore: 0.74, threshold: 0.5, activation: 0.48, weight: 0.2, contribution: 0.1, reason: "正向收益台阶连续出现。" },
  { name: "事件综合评分", rawScore: 0.7, threshold: 0, activation: 0.7, weight: 0.3, contribution: 0.21, reason: "由事件层因子聚合得出。" }
];

export const demoInput: AnalysisRequest = {
  headline: "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
  source: "Reuters",
  body:
    "Saudi Arabia announced a large solar storage project worth about $10B. The plan includes utility battery systems, solar inverters and grid AI coordination, potentially benefiting Tesla suppliers and clean-energy equipment companies."
};

export const demoAnalysis: AnalysisResponse = {
  analyzedAt: new Date().toISOString(),
  event: {
    eventType: "国际项目",
    sector: "新能源",
    country: "Saudi Arabia",
    projectAmountUsd: 10_000_000_000,
    companies: ["Tesla", "Enphase", "SolarEdge", "Nvidia"],
    keywords: ["solar", "battery", "energy storage", "project", "$10b", "Saudi Arabia", "Tesla"],
    source: "Reuters",
    sourceCredibility: 0.86
  },
  stocks: [
    {
      symbol: "TSLA",
      company: "Tesla",
      relation: "直接相关",
      direction: "利好",
      probability: 74,
      estimatedMove: "+2.0% ~ +5.5%",
      horizon: "3-10个交易日",
      relevance: 0.91,
      finalImpactScore: 0.74,
      factors: demoFactors,
      reluMomentum: demoReluMomentum,
      reluMetrics: demoReluMetrics,
      marketDataStatus: "UNAVAILABLE",
      marketData: null,
      marketConfirmation: null
    },
    {
      symbol: "ENPH",
      company: "Enphase",
      relation: "产业链相关",
      direction: "利好",
      probability: 63,
      estimatedMove: "+1.1% ~ +3.8%",
      horizon: "3-10个交易日",
      relevance: 0.72,
      finalImpactScore: 0.63,
      factors: demoFactors,
      reluMomentum: demoReluMomentum,
      reluMetrics: demoReluMetrics,
      marketDataStatus: "UNAVAILABLE",
      marketData: null,
      marketConfirmation: null
    },
    {
      symbol: "NVDA",
      company: "Nvidia",
      relation: "行业相关",
      direction: "利好",
      probability: 58,
      estimatedMove: "+0.8% ~ +2.9%",
      horizon: "3-10个交易日",
      relevance: 0.61,
      finalImpactScore: 0.58,
      factors: demoFactors,
      reluMomentum: demoReluMomentum,
      reluMetrics: demoReluMetrics,
      marketDataStatus: "UNAVAILABLE",
      marketData: null,
      marketConfirmation: null
    }
  ],
  reluMomentum: [
    { day: 1, price: 100, returnPct: 0, cumulativeReturn: 0, reluReturn: 0, reluMomentum: 0 },
    { day: 2, price: 101.2, returnPct: 1.19, cumulativeReturn: 1.19, reluReturn: 0.79, reluMomentum: 0.79 },
    { day: 3, price: 102.7, returnPct: 1.47, cumulativeReturn: 2.66, reluReturn: 1.07, reluMomentum: 1.86 },
    { day: 4, price: 101.9, returnPct: -0.78, cumulativeReturn: 1.88, reluReturn: 0, reluMomentum: 1.86 },
    { day: 5, price: 103.8, returnPct: 1.85, cumulativeReturn: 3.73, reluReturn: 1.45, reluMomentum: 3.31 },
    { day: 6, price: 104.6, returnPct: 0.77, cumulativeReturn: 4.5, reluReturn: 0.37, reluMomentum: 3.68 },
    { day: 7, price: 104.1, returnPct: -0.48, cumulativeReturn: 4.02, reluReturn: 0, reluMomentum: 3.68 },
    { day: 8, price: 104.0, returnPct: -0.1, cumulativeReturn: 3.92, reluReturn: 0, reluMomentum: 3.68 },
    { day: 9, price: 106.4, returnPct: 2.28, cumulativeReturn: 6.2, reluReturn: 1.88, reluMomentum: 5.56 },
    { day: 10, price: 108.1, returnPct: 1.58, cumulativeReturn: 7.78, reluReturn: 1.18, reluMomentum: 6.74 },
    { day: 11, price: 109.9, returnPct: 1.65, cumulativeReturn: 9.43, reluReturn: 1.25, reluMomentum: 7.99 },
    { day: 12, price: 111.2, returnPct: 1.18, cumulativeReturn: 10.61, reluReturn: 0.78, reluMomentum: 8.77 }
  ],
  reluMetrics: {
    threshold: 0.004,
    lookbackDays: 11,
    reluSlope: 0.00797,
    positiveDensity: 0.73,
    plateauRatio: 0.27,
    momentumPurity: 0.00425
  },
  reluFactors: [
    {
      name: "国际项目规模因子",
      rawScore: 0.88,
      threshold: 0.35,
      activation: 0.82,
      reason: "项目金额较大，可能带来订单和产业链需求。"
    },
    {
      name: "新闻源可信度因子",
      rawScore: 0.86,
      threshold: 0.45,
      activation: 0.75,
      reason: "权威媒体来源降低传播噪声。"
    },
    {
      name: "股票关联度因子",
      rawScore: 0.91,
      threshold: 0.4,
      activation: 0.85,
      reason: "公司与储能、电池、逆变器或电网 AI 关键词高度匹配。"
    },
    {
      name: "行业景气因子",
      rawScore: 0.78,
      threshold: 0.38,
      activation: 0.65,
      reason: "新能源和电网基础设施消息对相关标的敏感。"
    },
    {
      name: "市场确认因子",
      rawScore: 0.42,
      threshold: 0.55,
      activation: 0,
      reason: "尚未接入真实成交量和盘中价格，确认度偏低。"
    },
    {
      name: "ReLU 动量因子",
      rawScore: 0.74,
      threshold: 0.5,
      activation: 0.48,
      reason: "正向收益台阶连续出现，下跌日被截断为平台。"
    }
  ],
  explanation:
    "系统识别该新闻属于新能源国际项目事件，项目金额约 100 亿美元。TSLA 与储能、电池供应链存在较高关联，ENPH 与逆变器需求相关，NVDA 与电网 AI 调度和算力需求间接相关。新闻源可信度较高，项目规模、股票关联度和行业景气因子已明显激活，ReLU 动量曲线呈现台阶式上升，因此系统判断 TSLA 短期偏利好，概率高于其他相关股票。当前概率没有给到更高，主要因为市场确认因子尚未完全激活。",
  riskNote:
    "市场确认因子尚未完全激活，当前判断主要来自事件文本和产业链匹配。若后续成交量没有放大，或股价已经提前反应，实际影响可能低于模型估计。该结果不构成投资建议。"
};
