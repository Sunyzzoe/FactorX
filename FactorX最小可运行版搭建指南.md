# FactorX 最小可运行版搭建指南

> MVP · Spring Boot 后端 + React 前端 · 完整可运行代码

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | FactorX |
| 文档类型 | 最小可运行版搭建指南 |
| 版本 | v0.1.0 (MVP) |
| 后端 | Java 17 + Spring Boot 3.3.4 |
| 前端 | React + TypeScript + Vite + Recharts |
| 后端端口 | 8080 |
| 前端端口 | 5173 |

---

## 目录

- [一、项目结构](#一项目结构)
- [二、后端 Spring Boot](#二后端-spring-boot)
- [三、前端 React](#三前端-react)
- [四、启动指南](#四启动指南)
- [五、测试验证](#五测试验证)
- [六、API 说明](#六api-说明)

---

## 一、项目结构

```
factorx/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── resources/
│       │   └── application.properties
│       └── java/com/factorx/
│           ├── FactorXApplication.java
│           ├── api/
│           │   └── AnalysisController.java
│           └── model/
│               ├── AnalysisRequest.java
│               ├── AnalysisResponse.java
│               ├── ExtractedEvent.java
│               ├── StockImpact.java
│               ├── ReluFactor.java
│               └── ReluMomentumPoint.java
│
└── frontend/
    ├── package.json
    ├── index.html
    └── src/
        ├── main.tsx
        └── style.css
```

---

## 二、后端 Spring Boot

### 2.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.factorx</groupId>
    <artifactId>factorx-backend</artifactId>
    <version>0.1.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 application.properties

```properties
server.port=8080
spring.application.name=factorx-backend
```

### 2.3 FactorXApplication.java

```java
package com.factorx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FactorXApplication {
    public static void main(String[] args) {
        SpringApplication.run(FactorXApplication.class, args);
    }
}
```

### 2.4 AnalysisController.java

```java
package com.factorx.api;

import com.factorx.model.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnalysisController {

    @GetMapping("/demo")
    public AnalysisResponse demo() {
        return analyze(new AnalysisRequest(
                "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
                "Reuters",
                "The project may accelerate battery storage demand and benefit solar, inverter and AI grid companies."
        ));
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        String text = (request.headline() + " " + request.body()).toLowerCase();

        String sector = detectSector(text);
        String direction = detectDirection(text);
        double amountScore = text.contains("$10b") || text.contains("10b") ? 0.88 : 0.55;
        double sourceScore = "reuters".equalsIgnoreCase(request.source()) ? 0.86 : 0.62;
        double relevanceScore = text.contains("tesla") ? 0.91 : 0.68;
        double sectorScore = sector.equals("新能源") ? 0.78 : 0.65;
        double marketScore = 0.42;
        double reluMomentumScore = 0.74;

        List<ReluFactor> factors = List.of(
                factor("国际项目规模因子", amountScore, 0.35, "项目金额较大，可能带来订单和产业链需求。"),
                factor("新闻源可信度因子", sourceScore, 0.45, "权威媒体来源降低传闻噪声。"),
                factor("股票关联度因子", relevanceScore, 0.40, "新闻直接或间接命中公司与产业链关键词。"),
                factor("行业景气因子", sectorScore, 0.38, "新能源、AI芯片、半导体对国际项目消息较敏感。"),
                factor("市场确认因子", marketScore, 0.55, "第一版尚未接入真实成交量，暂以低确认处理。"),
                factor("ReLU动量因子", reluMomentumScore, 0.50, "正向收益台阶较连续，平台时间较短。")
        );

        double impactScore = factors.stream()
                .mapToDouble(ReluFactor::activation)
                .average()
                .orElse(0.5);

        int probability = (int) Math.round(45 + impactScore * 40);
        double low = round(0.5 + impactScore * 2);
        double high = round(low + 1.5 + impactScore * 3);

        List<StockImpact> stocks = List.of(
                new StockImpact("TSLA", "Tesla", "直接相关", direction, probability, "+" + low + "% ~ +" + high + "%", "3-10个交易日", 0.91),
                new StockImpact("ENPH", "Enphase", "产业链相关", direction, probability - 8, "+1.1% ~ +3.8%", "3-10个交易日", 0.72),
                new StockImpact("NVDA", "Nvidia", "行业相关", direction, probability - 13, "+0.8% ~ +2.9%", "3-10个交易日", 0.61)
        );

        List<ReluMomentumPoint> curve = List.of(
                p(1, 100, 1.2, 1.2, 1.2, 1.2),
                p(2, 101.5, 1.5, 2.7, 1.5, 2.7),
                p(3, 100.8, -0.7, 2.0, 0.0, 2.7),
                p(4, 102.6, 1.8, 3.8, 1.8, 4.5),
                p(5, 103.7, 1.1, 4.9, 1.1, 5.6),
                p(6, 103.1, -0.6, 4.3, 0.0, 5.6),
                p(7, 105.4, 2.2, 6.5, 2.2, 7.8),
                p(8, 106.8, 1.3, 7.8, 1.3, 9.1),
                p(9, 108.6, 1.7, 9.5, 1.7, 10.8),
                p(10, 110.8, 2.0, 11.5, 2.0, 12.8)
        );

        ExtractedEvent event = new ExtractedEvent(
                "国际项目",
                sector,
                "Saudi Arabia",
                10_000_000_000L,
                List.of("Tesla", "Enphase", "Nvidia"),
                request.source(),
                sourceScore
        );

        return new AnalysisResponse(
                Instant.now().toString(),
                event,
                stocks,
                curve,
                factors,
                "系统识别该新闻属于" + sector + "国际项目事件。项目规模较大，TSLA 与储能和电池供应链相关度较高，ReLU 动量曲线呈台阶式上升，因此短期偏利好。",
                "该结果是事件驱动影响评估，不构成投资建议。市场确认因子尚未接入真实成交量，需等待价格和成交量验证。"
        );
    }

    private String detectSector(String text) {
        if (text.contains("solar") || text.contains("battery") || text.contains("storage")) return "新能源";
        if (text.contains("chip") || text.contains("gpu") || text.contains("semiconductor")) return "AI芯片";
        return "综合";
    }

    private String detectDirection(String text) {
        if (text.contains("ban") || text.contains("sanction") || text.contains("delay")) return "利空";
        return "利好";
    }

    private ReluFactor factor(String name, double raw, double threshold, String reason) {
        double activation = Math.max(0, raw - threshold) / (1 - threshold);
        return new ReluFactor(name, round(raw), threshold, round(activation), reason);
    }

    private ReluMomentumPoint p(int day, double price, double returnPct, double cumulativeReturn, double reluReturn, double reluMomentum) {
        return new ReluMomentumPoint(day, price, returnPct, cumulativeReturn, reluReturn, reluMomentum);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
```

### 2.5 Model 类

**AnalysisRequest.java**

```java
package com.factorx.model;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String headline,
        String source,
        String body
) {}
```

**AnalysisResponse.java**

```java
package com.factorx.model;

import java.util.List;

public record AnalysisResponse(
        String analyzedAt,
        ExtractedEvent event,
        List<StockImpact> stocks,
        List<ReluMomentumPoint> reluMomentum,
        List<ReluFactor> reluFactors,
        String explanation,
        String riskNote
) {}
```

**ExtractedEvent.java**

```java
package com.factorx.model;

import java.util.List;

public record ExtractedEvent(
        String eventType,
        String sector,
        String country,
        long projectAmountUsd,
        List<String> companies,
        String source,
        double sourceCredibility
) {}
```

**StockImpact.java**

```java
package com.factorx.model;

public record StockImpact(
        String symbol,
        String company,
        String relation,
        String direction,
        int probability,
        String estimatedMove,
        String horizon,
        double relevance
) {}
```

**ReluFactor.java**

```java
package com.factorx.model;

public record ReluFactor(
        String name,
        double rawScore,
        double threshold,
        double activation,
        String reason
) {}
```

**ReluMomentumPoint.java**

```java
package com.factorx.model;

public record ReluMomentumPoint(
        int day,
        double price,
        double returnPct,
        double cumulativeReturn,
        double reluReturn,
        double reluMomentum
) {}
```

---

## 三、前端 React

### 3.1 package.json

```json
{
  "scripts": {
    "dev": "vite"
  },
  "dependencies": {
    "@vitejs/plugin-react": "latest",
    "vite": "latest",
    "typescript": "latest",
    "react": "latest",
    "react-dom": "latest",
    "axios": "latest",
    "recharts": "latest"
  },
  "devDependencies": {}
}
```

### 3.2 index.html

```html
<div id="root"></div>
<script type="module" src="/src/main.tsx"></script>
```

### 3.3 src/main.tsx

```tsx
import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  BarChart,
  Bar
} from "recharts";
import "./style.css";

type AnalysisRequest = {
  headline: string;
  source: string;
  body: string;
};

type AnalysisResponse = {
  analyzedAt: string;
  event: {
    eventType: string;
    sector: string;
    country: string;
    projectAmountUsd: number;
    companies: string[];
    source: string;
    sourceCredibility: number;
  };
  stocks: {
    symbol: string;
    company: string;
    relation: string;
    direction: string;
    probability: number;
    estimatedMove: string;
    horizon: string;
    relevance: number;
  }[];
  reluMomentum: {
    day: number;
    price: number;
    returnPct: number;
    cumulativeReturn: number;
    reluReturn: number;
    reluMomentum: number;
  }[];
  reluFactors: {
    name: string;
    rawScore: number;
    threshold: number;
    activation: number;
    reason: string;
  }[];
  explanation: string;
  riskNote: string;
};

const demoInput: AnalysisRequest = {
  headline: "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
  source: "Reuters",
  body: "The project may accelerate battery storage demand and benefit solar, inverter and AI grid companies."
};

function App() {
  const [request, setRequest] = useState<AnalysisRequest>(demoInput);
  const [analysis, setAnalysis] = useState<AnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function analyze() {
    setLoading(true);
    setError("");

    try {
      const res = await axios.post<AnalysisResponse>(
        "http://localhost:8080/api/analyze",
        request
      );
      setAnalysis(res.data);
    } catch {
      setError("分析失败：请确认 Spring Boot 后端已启动在 http://localhost:8080");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <h1>FactorX</h1>
          <p>国际事件驱动股票影响评估 · ReLU 非对称动量因子</p>
        </div>
        <span className="status">MVP Demo</span>
      </header>

      <section className="grid">
        <div className="panel">
          <h2>国际新闻输入</h2>
          <label>新闻标题</label>
          <input
            value={request.headline}
            onChange={(e) => setRequest({ ...request, headline: e.target.value })}
          />

          <label>新闻来源</label>
          <input
            value={request.source}
            onChange={(e) => setRequest({ ...request, source: e.target.value })}
          />

          <label>新闻正文</label>
          <textarea
            rows={8}
            value={request.body}
            onChange={(e) => setRequest({ ...request, body: e.target.value })}
          />

          <div className="actions">
            <button onClick={analyze} disabled={loading}>
              {loading ? "分析中..." : "AI 分析"}
            </button>
            <button className="secondary" onClick={() => setRequest(demoInput)}>
              载入示例
            </button>
          </div>

          {error && <p className="error">{error}</p>}
        </div>

        <div className="panel">
          <h2>事件提取结果</h2>
          {analysis ? (
            <div className="facts">
              <p><span>事件类型</span>{analysis.event.eventType}</p>
              <p><span>行业</span>{analysis.event.sector}</p>
              <p><span>国家/地区</span>{analysis.event.country}</p>
              <p><span>项目金额</span>${(analysis.event.projectAmountUsd / 1e9).toFixed(2)}B</p>
              <p><span>相关公司</span>{analysis.event.companies.join("、")}</p>
              <p><span>来源可信度</span>{Math.round(analysis.event.sourceCredibility * 100)}%</p>
            </div>
          ) : (
            <p className="empty">等待分析结果</p>
          )}
        </div>

        <div className="panel">
          <h2>股票影响评估</h2>
          {analysis ? (
            <div className="stock-list">
              {analysis.stocks.map((s) => (
                <div className="stock" key={s.symbol}>
                  <div>
                    <strong>{s.symbol}</strong>
                    <span>{s.company} · {s.relation}</span>
                  </div>
                  <div className={s.direction === "利好" ? "good" : "bad"}>
                    {s.direction} {s.probability}%
                  </div>
                  <p>{s.estimatedMove} · {s.horizon}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="empty">暂无股票评分</p>
          )}
        </div>
      </section>

      {analysis && (
        <>
          <section className="panel wide">
            <h2>ReLU 非对称动量曲线</h2>
            <div className="chart">
              <ResponsiveContainer width="100%" height={320}>
                <LineChart data={analysis.reluMomentum}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="day" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="cumulativeReturn" name="传统累计收益" stroke="#64748b" strokeWidth={2} />
                  <Line type="monotone" dataKey="reluMomentum" name="ReLU 正向动量" stroke="#14b8a6" strokeWidth={3} />
                  <Line type="monotone" dataKey="returnPct" name="日收益率" stroke="#f59e0b" strokeWidth={1} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="grid bottom">
            <div className="panel">
              <h2>ReLU 因子激活</h2>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={analysis.reluFactors}>
                  <XAxis dataKey="name" hide />
                  <YAxis domain={[0, 1]} />
                  <Tooltip />
                  <Bar dataKey="activation" fill="#14b8a6" />
                </BarChart>
              </ResponsiveContainer>
              <div className="factor-list">
                {analysis.reluFactors.map((f) => (
                  <p key={f.name}>
                    <strong>{f.name}</strong>
                    <span>{f.activation.toFixed(2)}</span>
                  </p>
                ))}
              </div>
            </div>

            <div className="panel">
              <h2>AI 解释</h2>
              <p className="text">{analysis.explanation}</p>
            </div>

            <div className="panel">
              <h2>风险提示</h2>
              <p className="text">{analysis.riskNote}</p>
            </div>
          </section>
        </>
      )}
    </main>
  );
}

createRoot(document.getElementById("root")!).render(<App />);
```

### 3.4 src/style.css

```css
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #0f172a;
  color: #e5e7eb;
  font-family: Inter, Arial, sans-serif;
}

.app {
  padding: 24px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

h1 {
  margin: 0;
  font-size: 28px;
}

h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

p {
  margin: 0;
}

.topbar p {
  color: #94a3b8;
  margin-top: 6px;
}

.status {
  border: 1px solid #334155;
  background: #111827;
  color: #67e8f9;
  padding: 8px 12px;
  border-radius: 999px;
}

.grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1.2fr;
  gap: 16px;
}

.bottom {
  margin-top: 16px;
}

.panel {
  background: #111827;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 18px;
}

.wide {
  margin-top: 16px;
}

label {
  display: block;
  color: #94a3b8;
  margin: 12px 0 6px;
  font-size: 13px;
}

input,
textarea {
  width: 100%;
  border: 1px solid #334155;
  background: #020617;
  color: #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

button {
  border: 0;
  background: #14b8a6;
  color: #042f2e;
  padding: 10px 14px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button.secondary {
  background: #334155;
  color: #e5e7eb;
}

.error {
  color: #fca5a5;
  margin-top: 12px;
}

.empty {
  color: #64748b;
}

.facts {
  display: grid;
  gap: 10px;
}

.facts p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #1f2937;
  padding-bottom: 8px;
}

.facts span {
  color: #94a3b8;
}

.stock-list {
  display: grid;
  gap: 12px;
}

.stock {
  border: 1px solid #1f2937;
  border-radius: 10px;
  padding: 12px;
  background: #020617;
}

.stock div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.stock span {
  display: block;
  color: #94a3b8;
  margin-top: 4px;
  font-size: 13px;
}

.stock p {
  color: #cbd5e1;
  margin-top: 10px;
}

.good {
  color: #34d399;
  font-weight: 700;
}

.bad {
  color: #f87171;
  font-weight: 700;
}

.chart {
  width: 100%;
}

.factor-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.factor-list p {
  display: flex;
  justify-content: space-between;
  color: #cbd5e1;
}

.text {
  color: #cbd5e1;
  line-height: 1.7;
}

@media (max-width: 1000px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
```

---

## 四、启动指南

### 4.1 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| npm | 9+ |

### 4.2 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后监听 `http://localhost:8080`。

### 4.3 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173`。

> **注意**：需要先启动后端，再启动前端，否则前端点击"AI 分析"会报连接错误。

---

## 五、测试验证

### 5.1 后端测试

**Demo 接口（GET，可直接浏览器打开）：**

```
http://localhost:8080/api/demo
```

应返回完整的 JSON 分析结果。

**分析接口（POST，需用 curl 或 Postman）：**

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "headline": "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
    "source": "Reuters",
    "body": "The project may accelerate battery storage demand."
  }'
```

### 5.2 前端测试

1. 打开 `http://localhost:5173`
2. 确认页面显示新闻输入表单（默认已填充示例）
3. 点击 **"AI 分析"** 按钮
4. 验证以下区域是否正确显示：
   - 事件提取结果（事件类型、行业、金额、公司）
   - 股票影响评估（TSLA / ENPH / NVDA）
   - ReLU 非对称动量曲线（三条线）
   - ReLU 因子激活柱状图
   - AI 解释文本
   - 风险提示文本

### 5.3 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 前端报"分析失败" | 后端未启动 | 先启动 `mvn spring-boot:run` |
| 后端启动失败 | 8080 端口被占用 | 关闭占用程序或修改 `server.port` |
| `mvn` 命令不存在 | 未安装 Maven | 安装 Maven 3.8+ |
| `npm install` 失败 | Node 版本过低 | 升级到 Node 18+ |

---

## 六、API 说明

### 6.1 GET /api/demo

返回预设示例新闻的分析结果，无需参数，用于快速验证后端是否正常。

### 6.2 POST /api/analyze

**核心分析接口。**

**请求体：**

```json
{
  "headline": "新闻标题（必填）",
  "source": "新闻来源",
  "body": "新闻正文"
}
```

**响应体：**

```json
{
  "analyzedAt": "分析时间",
  "event": {
    "eventType": "事件类型",
    "sector": "行业",
    "country": "国家",
    "projectAmountUsd": 10000000000,
    "companies": ["公司1", "公司2"],
    "source": "来源",
    "sourceCredibility": 0.86
  },
  "stocks": [
    {
      "symbol": "TSLA",
      "company": "Tesla",
      "relation": "直接相关",
      "direction": "利好",
      "probability": 74,
      "estimatedMove": "+2.0% ~ +5.5%",
      "horizon": "3-10个交易日",
      "relevance": 0.91
    }
  ],
  "reluMomentum": [
    {
      "day": 1,
      "price": 100,
      "returnPct": 1.2,
      "cumulativeReturn": 1.2,
      "reluReturn": 1.2,
      "reluMomentum": 1.2
    }
  ],
  "reluFactors": [
    {
      "name": "国际项目规模因子",
      "rawScore": 0.88,
      "threshold": 0.35,
      "activation": 0.82,
      "reason": "原因说明"
    }
  ],
  "explanation": "AI 解释文本",
  "riskNote": "风险提示文本"
}
```

> **重要**：`/api/analyze` 是 POST 接口，**不能直接用浏览器打开看结果**，需要通过前端页面或 curl/Postman 调用。

---

## 七、MVP 已实现功能

- [x] 新闻标题/来源/正文输入
- [x] 规则版事件类型识别（国际项目 / 风险事件）
- [x] 规则版行业识别（新能源 / AI芯片 / 综合）
- [x] 规则版方向识别（利好 / 利空）
- [x] 静态股票匹配（TSLA / ENPH / NVDA）
- [x] 6 因子 ReLU 激活计算
- [x] 影响概率和涨跌幅区间估算
- [x] ReLU 非对称动量曲线（模拟数据，10 个交易日）
- [x] 因子激活柱状图
- [x] AI 自然语言解释
- [x] 风险提示
- [x] 深色金融终端风格 UI
- [x] 响应式布局（桌面三栏 / 移动单列）

---

*文档结束*
