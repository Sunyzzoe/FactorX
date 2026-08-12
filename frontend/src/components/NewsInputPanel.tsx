import type { AnalysisRequest } from "../types/analysis";

type NewsInputPanelProps = {
  request: AnalysisRequest;
  isAnalyzing: boolean;
  error: string | null;
  onChange: (request: AnalysisRequest) => void;
  onAnalyze: () => void;
  onLoadDemo: () => void;
  onClear: () => void;
};

export function NewsInputPanel({
  request,
  isAnalyzing,
  error,
  onChange,
  onAnalyze,
  onLoadDemo,
  onClear
}: NewsInputPanelProps) {
  const disabled = isAnalyzing || !request.headline.trim();

  return (
    <section className="panel input-panel">
      <div className="panel-head">
        <div>
          <h2>新闻输入</h2>
          <p>粘贴一条国际新闻，触发事件影响评估。</p>
        </div>
        <div className="panel-actions">
          <button className="secondary" type="button" onClick={onLoadDemo}>
            载入示例
          </button>
          <button className="ghost" type="button" onClick={onClear}>
            清空
          </button>
        </div>
      </div>

      <label htmlFor="headline">标题</label>
      <input
        id="headline"
        value={request.headline}
        onChange={(event) => onChange({ ...request, headline: event.target.value })}
        placeholder="Saudi Arabia announces $10B solar storage project..."
      />

      <label htmlFor="source">来源</label>
      <input
        id="source"
        value={request.source}
        onChange={(event) => onChange({ ...request, source: event.target.value })}
        placeholder="Reuters / Bloomberg / CNBC"
      />

      <label htmlFor="body">正文</label>
      <textarea
        id="body"
        rows={9}
        value={request.body}
        onChange={(event) => onChange({ ...request, body: event.target.value })}
        placeholder="粘贴新闻正文或摘要"
      />

      <button className="primary wide" type="button" onClick={onAnalyze} disabled={disabled}>
        {isAnalyzing ? "分析中..." : "AI 分析"}
      </button>

      {error ? <div className="error-banner">{error}</div> : null}
    </section>
  );
}
