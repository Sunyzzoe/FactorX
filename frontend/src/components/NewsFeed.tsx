import { useCallback, useEffect, useState } from "react";
import { fetchNews } from "../api/analysisApi";
import type { NewsItem } from "../types/analysis";

type NewsFeedProps = {
  onSelect: (item: NewsItem) => void;
};

export function NewsFeed({ onSelect }: NewsFeedProps) {
  const [items, setItems] = useState<NewsItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const result = await fetchNews({ page: 0, size: 12 });
      setItems(result.content);
      setError(null);
    } catch {
      setError("新闻源暂时不可用");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const timer = window.setInterval(() => void load(), 60_000);
    return () => window.clearInterval(timer);
  }, [load]);

  return (
    <section className="panel news-feed-panel">
      <div className="panel-head">
        <div>
          <h2>实时新闻流</h2>
          <p>接入新闻源后自动更新，点击新闻可载入分析。</p>
        </div>
        <button className="ghost" type="button" onClick={() => void load()} title="刷新新闻">
          刷新
        </button>
      </div>

      {isLoading ? <div className="empty-state">正在加载新闻...</div> : null}
      {!isLoading && error ? <div className="empty-state">{error}</div> : null}
      {!isLoading && !error && items.length === 0 ? (
        <div className="empty-state">暂无真实新闻。请启用 RSS 或 Finnhub 数据源。</div>
      ) : null}
      <div className="news-feed-list">
        {items.map((item) => (
          <button className="news-feed-item" type="button" key={item.id} onClick={() => onSelect(item)}>
            <strong>{item.title}</strong>
            <span>
              {item.source || item.sourceCode || "未知来源"} · {formatDate(item.publishedAt)} · {statusLabel(item.status)}
            </span>
          </button>
        ))}
      </div>
    </section>
  );
}

function formatDate(value?: string) {
  if (!value) return "时间待确认";
  return new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" })
    .format(new Date(value));
}

function statusLabel(status: string) {
  return {
    RECEIVED: "待分析",
    ANALYZING: "处理中",
    ANALYZED: "已分析",
    FAILED: "分析失败",
    DUPLICATE: "重复"
  }[status] ?? status;
}
