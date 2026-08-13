import { formatAmount, formatPercent } from "./formatters";
import type { ExtractedEvent } from "../types/analysis";

type EventExtractionPanelProps = {
  event: ExtractedEvent | null;
};

export function EventExtractionPanel({ event }: EventExtractionPanelProps) {
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>事件提取结果</h2>
          <p>结构化识别新闻中的关键变量。</p>
        </div>
      </div>

      {event ? (
        <div className="facts">
          <Fact label="事件类型" value={event.eventType || "待确认"} />
          <Fact label="行业" value={event.sector || "待确认"} />
          <Fact label="国家/地区" value={event.country || "待确认"} />
          <Fact label="项目金额" value={formatAmount(event.projectAmountUsd)} />
          <Fact label="相关公司" value={event.companies.length ? event.companies.join(", ") : "待确认"} />
          <Fact label="命中关键词" value={event.keywords.length ? event.keywords.join(", ") : "待确认"} />
          <Fact label="新闻源可信度" value={formatPercent(event.sourceCredibility)} />
        </div>
      ) : (
        <div className="empty-state">等待输入新闻并点击 AI 分析。</div>
      )}
    </section>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="fact-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
