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
            <div className="selected-stock">
              <Metric label="当前选中" value={selectedStock.symbol} />
              <Metric label="周期" value={selectedStock.horizon} />
              <Metric label="关联度" value={`${Math.round(selectedStock.relevance * 100)}%`} />
            </div>
          ) : null}
        </>
      ) : (
        <div className="empty-state">暂无股票影响结果。</div>
      )}
    </section>
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
