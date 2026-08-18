import type { FactorScore, StockImpact } from "../types/analysis";

type FactorActivationPanelProps = {
  factors: FactorScore[];
  selectedStock: StockImpact | null;
};

export function FactorActivationPanel({ factors, selectedStock }: FactorActivationPanelProps) {
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>因子激活区</h2>
          <p>{selectedStock ? `当前解释对象：${selectedStock.symbol}` : "等待分析结果"}</p>
        </div>
      </div>

      {factors.length ? (
        <div className="factor-list">
          {factors.map((factor) => (
            <div className="factor-row" key={factor.name}>
              <div className="factor-copy">
                <div className="factor-title">
                  <strong>{factor.name}</strong>
                  <span>{factor.activation.toFixed(2)}</span>
                </div>
                <div className="activation-track" aria-label={`${factor.name} activation`}>
                  <i style={{ width: `${Math.round(factor.activation * 100)}%` }} />
                </div>
                <div className="factor-meta">
                  <span>Raw {factor.rawScore.toFixed(2)}</span>
                  <span>Threshold {factor.threshold.toFixed(2)}</span>
                  <span>Activation {factor.activation.toFixed(2)}</span>
                  {factor.weight > 0 ? <span>贡献 {factor.contribution.toFixed(2)}</span> : null}
                </div>
                <p>{factor.reason}</p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">暂无因子激活结果。</div>
      )}
    </section>
  );
}
