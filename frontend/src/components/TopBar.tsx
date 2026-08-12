import type { AnalysisResponse } from "../types/analysis";

type TopBarProps = {
  analysis: AnalysisResponse | null;
  usingFallback: boolean;
};

export function TopBar({ analysis, usingFallback }: TopBarProps) {
  const lastAnalyzed = analysis
    ? new Date(analysis.analyzedAt).toLocaleString("zh-CN", {
        hour12: false,
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
      })
    : "尚未分析";

  return (
    <header className="topbar">
      <div className="brand-block">
        <div className="brand-row">
          <h1>FactorX</h1>
          <span className="status-chip accent">MVP</span>
        </div>
        <p>国际事件驱动股票影响评估工作台</p>
      </div>

      <div className="top-meta" aria-label="状态标签">
        <span className="status-chip">{usingFallback ? "模拟数据" : "实时接口"}</span>
        <span className="status-chip">规则模型</span>
        <span className="status-chip">ReLU 因子引擎</span>
        <span className="status-chip muted">最后分析 {lastAnalyzed}</span>
      </div>
    </header>
  );
}
