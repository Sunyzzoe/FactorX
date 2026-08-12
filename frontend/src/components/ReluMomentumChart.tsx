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
import type { ReluMomentumPoint, StockImpact } from "../types/analysis";

type ReluMomentumChartProps = {
  data: ReluMomentumPoint[];
  selectedStock: StockImpact | null;
};

export function ReluMomentumChart({ data, selectedStock }: ReluMomentumChartProps) {
  const positiveDays = data.filter((point) => point.reluReturn > 0).length;
  const flatDays = data.filter((point) => point.reluReturn === 0).length;
  const slope = data.length ? data[data.length - 1].reluMomentum / data.length : 0;
  const purity = data.length ? positiveDays / data.length : 0;

  return (
    <section className="panel chart-panel">
      <div className="panel-head chart-head">
        <div>
          <h2>ReLU 非对称动量曲线</h2>
          <p>{selectedStock ? `${selectedStock.symbol} ${selectedStock.company}` : "等待选择股票"}</p>
        </div>
        <div className="chart-metrics">
          <Metric label="动量斜率" value={`${slope.toFixed(2)}% / 日`} />
          <Metric label="正收益密度" value={`${Math.round(purity * 100)}%`} />
          <Metric label="平台天数" value={`${flatDays} 天`} />
          <Metric label="动量纯度" value={purity.toFixed(2)} />
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
              <Line type="monotone" dataKey="price" name="原始净值" stroke="#58a6ff" strokeWidth={2} dot={false} />
              <Line
                type="monotone"
                dataKey="cumulativeReturn"
                name="传统累计收益"
                stroke="#f2b84b"
                strokeWidth={2}
                dot={false}
              />
              <Line
                type="stepAfter"
                dataKey="reluMomentum"
                name="ReLU 正向动量"
                stroke="#18c7a7"
                strokeWidth={3}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="empty-state tall">暂无 ReLU 曲线数据。</div>
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
