import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import type { ReluMetrics, ReluMomentumPoint, StockImpact } from "../types/analysis";

type ReluMomentumChartProps = {
  data: ReluMomentumPoint[];
  metrics: ReluMetrics | null;
  selectedStock: StockImpact | null;
};

export function ReluMomentumChart({ data, metrics, selectedStock }: ReluMomentumChartProps) {
  return (
    <section className="panel chart-panel">
      <div className="panel-head chart-head">
        <div>
          <h2>ReLU Momentum</h2>
          <p>{selectedStock ? `${selectedStock.symbol} ${selectedStock.company}` : "No stock selected"}</p>
        </div>
        <div className="chart-metrics">
          <Metric label="ReLU slope" value={formatPercent(metrics?.reluSlope)} />
          <Metric label="Positive density" value={formatPercent(metrics?.positiveDensity)} />
          <Metric label="Plateau risk" value={formatPercent(metrics?.plateauRatio)} />
          <Metric label="Alpha purity" value={formatPercent(metrics?.momentumPurity)} />
          <Metric label="Threshold" value={formatPercent(metrics?.threshold)} />
        </div>
      </div>

      {data.length ? (
        <div className="chart-box">
          <ResponsiveContainer width="100%" height={340}>
            <LineChart data={data} margin={{ left: 8, right: 20, top: 8, bottom: 8 }}>
              <CartesianGrid stroke="#23334f" strokeDasharray="3 3" />
              <XAxis dataKey="day" stroke="#8ea2bf" tickLine={false} />
              <YAxis stroke="#8ea2bf" tickLine={false} />
              <Tooltip
                contentStyle={{
                  background: "#08111f",
                  border: "1px solid #263854",
                  borderRadius: 8,
                  color: "#e6edf7"
                }}
              />
              <Legend />
              <Line type="monotone" dataKey="price" name="Price" stroke="#58a6ff" strokeWidth={2} dot={false} />
              <Line
                type="monotone"
                dataKey="cumulativeReturn"
                name="Cumulative return"
                stroke="#f2b84b"
                strokeWidth={2}
                dot={false}
              />
              <Line
                type="stepAfter"
                dataKey="reluMomentum"
                name="ReLU positive momentum"
                stroke="#18c7a7"
                strokeWidth={3}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="empty-state tall">No ReLU momentum data.</div>
      )}
    </section>
  );
}

function formatPercent(value: number | undefined) {
  return value === undefined ? "--" : `${(value * 100).toFixed(2)}%`;
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
