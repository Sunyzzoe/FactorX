type ExplanationPanelProps = {
  explanation?: string;
};

export function ExplanationPanel({ explanation }: ExplanationPanelProps) {
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>AI 解释</h2>
          <p>匹配原因、方向、概率和未确认因素。</p>
        </div>
      </div>
      <p className="body-copy">{explanation || "暂无解释结果。"}</p>
    </section>
  );
}
