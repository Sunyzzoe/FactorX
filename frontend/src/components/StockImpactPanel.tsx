import { normalizeDirection } from "./formatters";
import type { StockImpact } from "../types/analysis";

type StockImpactPanelProps = {
  stocks: StockImpact[];
  selectedSymbol: string | null;
  onSelect: (symbol: string) => void;
};

export function StockImpactPanel({ stocks, selectedSymbol, onSelect }: StockImpactPanelProps) {
  const selectedStock = stocks.find((stock) => stock.symbol === selectedSymbol) ?? stocks[0] ?? null;

  return (
    <section className="panel stock-panel">
      <div className="panel-head">
        <div>
          <h2>股票影响评估</h2>
          <p>点击股票切换下方曲线和因子解释。</p>
        </div>
      </div>

      {stocks.length ? (
        <>
          <div className="stock-table">
            <div className="stock-table-head">
              <span>股票</span>
              <span>关系</span>
              <span>方向</span>
              <span>概率</span>
              <span>预计影响</span>
            </div>
            {stocks.map((stock) => {
              const active = stock.symbol === selectedSymbol;
              const directionClass = normalizeDirection(stock.direction);

              return (
                <button
                  className={`stock-row ${active ? "active" : ""}`}
                  key={stock.symbol}
                  type="button"
                  onClick={() => onSelect(stock.symbol)}
                >
                  <span>
                    <strong>{stock.symbol}</strong>
                    <small>{stock.company}</small>
                  </span>
                  <span>{stock.relation}</span>
                  <span className={`direction ${directionClass}`}>{stock.direction}</span>
                  <span className="probability">
                    <b>{stock.probability}%</b>
                    <i style={{ width: `${stock.probability}%` }} />
                  </span>
                  <span>{stock.estimatedMove}</span>
                </button>
              );
            })}
          </div>

          {selectedStock ? (
            <>
              <div className="selected-stock">
                <Metric label="当前选中" value={selectedStock.symbol} />
                <Metric label="周期" value={selectedStock.horizon} />
                <Metric label="关联度" value={`${Math.round(selectedStock.relevance * 100)}%`} />
                <Metric label="行情状态" value={marketStatus(selectedStock.marketDataStatus)} />
                {selectedStock.marketData ? (
                  <>
                    <Metric label="1日收益" value={percent(selectedStock.marketData.return1d)} />
                    <Metric label="量比" value={`${selectedStock.marketData.volumeRatio.toFixed(2)}x`} />
                    <Metric label="20日波动" value={percent(selectedStock.marketData.annualizedVolatility)} />
                    <Metric
                      label="市场确认"
                      value={`${Math.round((selectedStock.marketConfirmation?.score ?? 0) * 100)}%`}
                    />
                  </>
                ) : null}
              </div>
              {selectedStock.marketConfirmation?.riskNote ? (
                <p className="market-risk">{selectedStock.marketConfirmation.riskNote}</p>
              ) : null}
            </>
          ) : null}
        </>
      ) : (
        <div className="empty-state">暂无股票影响结果。</div>
      )}
    </section>
  );
}

function marketStatus(status: string) {
  if (status === "AVAILABLE") return "已接入";
  if (status === "INSUFFICIENT_DATA") return "数据不足";
  return "不可用";
}

function percent(value: number) {
  return `${(value * 100).toFixed(2)}%`;
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
