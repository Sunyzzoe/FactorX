import { useMemo, useState } from "react";
import { analyzeNews } from "../api/analysisApi";
import { EventExtractionPanel } from "../components/EventExtractionPanel";
import { ExplanationPanel } from "../components/ExplanationPanel";
import { FactorActivationPanel } from "../components/FactorActivationPanel";
import { formatAmount, formatPercent } from "../components/formatters";
import { NewsInputPanel } from "../components/NewsInputPanel";
import { ReluMomentumChart } from "../components/ReluMomentumChart";
import { RiskNotePanel } from "../components/RiskNotePanel";
import { StockImpactPanel } from "../components/StockImpactPanel";
import { TopBar } from "../components/TopBar";
import { demoAnalysis, demoInput } from "../data/demoAnalysis";
import type { AnalysisRequest, AnalysisResponse } from "../types/analysis";

export function DashboardPage() {
  const [request, setRequest] = useState<AnalysisRequest>(demoInput);
  const [analysis, setAnalysis] = useState<AnalysisResponse | null>(demoAnalysis);
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(demoAnalysis.stocks[0]?.symbol ?? null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [usingFallback, setUsingFallback] = useState(true);

  const selectedStock = useMemo(
    () => analysis?.stocks.find((stock) => stock.symbol === selectedSymbol) ?? analysis?.stocks[0] ?? null,
    [analysis, selectedSymbol]
  );

  async function handleAnalyze() {
    setIsAnalyzing(true);
    setError(null);

    try {
      const result = await analyzeNews(request);
      setAnalysis(result);
      setSelectedSymbol(result.stocks[0]?.symbol ?? null);
      setUsingFallback(false);
    } catch {
      setAnalysis(demoAnalysis);
      setSelectedSymbol(demoAnalysis.stocks[0]?.symbol ?? null);
      setUsingFallback(true);
      setError("后端服务不可用，已自动切换为模拟数据。");
    } finally {
      setIsAnalyzing(false);
    }
  }

  function handleLoadDemo() {
    setRequest(demoInput);
    setAnalysis(demoAnalysis);
    setSelectedSymbol(demoAnalysis.stocks[0]?.symbol ?? null);
    setUsingFallback(true);
    setError(null);
  }

  function handleClear() {
    setRequest({ headline: "", source: "", body: "" });
    setAnalysis(null);
    setSelectedSymbol(null);
    setUsingFallback(true);
    setError(null);
  }

  return (
    <main className="app-shell">
      <TopBar analysis={analysis} usingFallback={usingFallback} />

      <section className="summary-band" aria-label="分析摘要">
        <div>
          <span>核心链路</span>
          <strong>输入国际新闻 → 提取事件 → 匹配股票 → ReLU 曲线 → 解释与风险</strong>
        </div>
        <div className="summary-metrics">
          <Metric label="行业" value={analysis?.event.sector ?? "待确认"} />
          <Metric label="项目金额" value={formatAmount(analysis?.event.projectAmountUsd)} />
          <Metric label="可信度" value={analysis ? formatPercent(analysis.event.sourceCredibility) : "待确认"} />
        </div>
      </section>

      <section className="workspace-grid">
        <NewsInputPanel
          request={request}
          isAnalyzing={isAnalyzing}
          error={error}
          onChange={setRequest}
          onAnalyze={handleAnalyze}
          onLoadDemo={handleLoadDemo}
          onClear={handleClear}
        />
        <EventExtractionPanel event={analysis?.event ?? null} />
        <StockImpactPanel
          stocks={analysis?.stocks ?? []}
          selectedSymbol={selectedSymbol}
          onSelect={setSelectedSymbol}
        />
      </section>

      <ReluMomentumChart
        data={analysis?.reluMomentum ?? []}
        metrics={analysis?.reluMetrics ?? null}
        selectedStock={selectedStock}
      />

      <section className="bottom-grid">
        <FactorActivationPanel factors={analysis?.reluFactors ?? []} selectedStock={selectedStock} />
        <ExplanationPanel explanation={analysis?.explanation} />
        <RiskNotePanel riskNote={analysis?.riskNote} />
      </section>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
