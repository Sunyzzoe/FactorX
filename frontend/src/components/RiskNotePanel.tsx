type RiskNotePanelProps = {
  riskNote?: string;
};

const riskTags = ["市场未确认", "可能已定价", "消息源待验证", "行业联动不足", "成交量不足"];

export function RiskNotePanel({ riskNote }: RiskNotePanelProps) {
  return (
    <section className="panel risk-panel">
      <div className="panel-head">
        <div>
          <h2>风险提示</h2>
          <p>金融产品必须保留风险约束。</p>
        </div>
      </div>
      <div className="risk-tags">
        {riskTags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>
      <p className="body-copy">{riskNote || "暂无风险提示。"}</p>
    </section>
  );
}
