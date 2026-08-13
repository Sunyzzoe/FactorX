export type AnalysisRequest = {
  headline: string;
  source: string;
  body: string;
};

export type ExtractedEvent = {
  eventType: string;
  sector: string;
  country?: string;
  projectAmountUsd?: number;
  companies: string[];
  keywords: string[];
  source: string;
  sourceCredibility: number;
};

export type StockDirection = "利好" | "利空" | "中性" | string;

export type StockImpact = {
  symbol: string;
  company: string;
  relation: string;
  direction: StockDirection;
  probability: number;
  estimatedMove: string;
  horizon: string;
  relevance: number;
};

export type ReluMomentumPoint = {
  day: number;
  price: number;
  returnPct: number;
  cumulativeReturn: number;
  reluReturn: number;
  reluMomentum: number;
};

export type ReluFactor = {
  name: string;
  rawScore: number;
  threshold: number;
  activation: number;
  reason: string;
};

export type AnalysisResponse = {
  analyzedAt: string;
  event: ExtractedEvent;
  stocks: StockImpact[];
  reluMomentum: ReluMomentumPoint[];
  reluFactors: ReluFactor[];
  explanation: string;
  riskNote: string;
};
