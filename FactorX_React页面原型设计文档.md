# FactorX React 页面原型设计文档

> 第一版 MVP · AI 金融事件分析工作台

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | FactorX |
| 文档类型 | 前端页面原型设计 |
| 版本 | v1.0 (MVP) |
| 页面定位 | AI 金融事件分析工作台 |
| 技术栈 | React + TypeScript + Vite + Tailwind CSS |

---

## 目录

- [一、页面定位](#一页面定位)
- [二、整体页面布局](#二整体页面布局)
- [三、核心组件拆分](#三核心组件拆分)
- [四、页面组件详细设计](#四页面组件详细设计)
- [五、核心模块：ReluMomentumChart](#五核心模块relumomentumchart)
- [六、FactorActivationPanel 因子激活区](#六factoractivationpanel-因子激活区)
- [七、ExplanationPanel AI 解释区](#七explanationpanel-ai-解释区)
- [八、RiskNotePanel 风险提示区](#八risknotepanel-风险提示区)
- [九、前端状态设计](#九前端状态设计)
- [十、接口调用设计](#十接口调用设计)
- [十一、页面视觉风格](#十一页面视觉风格)
- [十二、第一版验收标准](#十二第一版验收标准)

---

## 一、页面定位

第一版不要做成复杂 App，先做一个**核心工作台**：

> **FactorX AI 金融事件分析工作台**

用户进入页面后，只做一件事：

```
粘贴一条国际新闻
    ↓
点击分析
    ↓
看到股票影响概率、涨跌幅、ReLU 因子和曲线
```

**核心链路：**

```
输入国际新闻
  → 提取事件
  → 匹配股票
  → 计算影响概率
  → 展示 ReLU 非对称动量曲线
  → 输出解释和风险提示
```

---

## 二、整体页面布局

### 2.1 桌面端布局（三栏 + 底部曲线区）

```
┌──────────────────────────────────────────────────────────────────┐
│  FactorX        国际事件驱动股票影响评估       模拟数据/实时中      │
├──────────────────┬───────────────────────┬───────────────────────┤
│  新闻输入/新闻流  │  事件提取结果           │  股票影响评估           │
│                  │                       │                       │
│  标题             │  事件类型               │  TSLA  利好  74%        │
│  来源             │  行业                   │  NVDA  利好  68%        │
│  正文             │  项目金额               │  ENPH  利好  61%        │
│  AI分析按钮        │  相关公司               │  预估涨跌幅              │
├──────────────────┴───────────────────────┴───────────────────────┤
│  ReLU 非对称动量曲线                                               │
│  原始净值 / 传统累计收益 / ReLU 正向动量                            │
├──────────────────────────────────────────────────────────────────┤
│  因子激活条 + AI解释 + 风险提示                                     │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 手机端布局（单列）

```
新闻输入
  ↓
事件提取
  ↓
股票影响
  ↓
ReLU 曲线
  ↓
因子解释
  ↓
风险提示
```

---

## 三、核心组件拆分

### 3.1 目录结构

```
src/
  App.tsx
  pages/
    DashboardPage.tsx

  components/
    TopBar.tsx
    NewsInputPanel.tsx
    EventExtractionPanel.tsx
    StockImpactPanel.tsx
    ReluMomentumChart.tsx
    FactorActivationPanel.tsx
    ExplanationPanel.tsx
    RiskNotePanel.tsx

  api/
    analysisApi.ts

  types/
    analysis.ts

  data/
    demoAnalysis.ts
```

> 第一版主页面只需要 `DashboardPage.tsx`。

### 3.2 组件职责一览

| 组件 | 职责 |
|------|------|
| `TopBar` | 品牌、数据状态、最后分析时间 |
| `NewsInputPanel` | 新闻标题/来源/正文输入，触发分析 |
| `EventExtractionPanel` | 展示结构化事件抽取结果 |
| `StockImpactPanel` | 展示股票影响列表，支持选中切换 |
| `ReluMomentumChart` | ReLU 非对称动量曲线（核心模块） |
| `FactorActivationPanel` | 因子激活条展示 |
| `ExplanationPanel` | AI 自然语言解释 |
| `RiskNotePanel` | 风险提示与免责声明 |

---

## 四、页面组件详细设计

### 4.1 TopBar 顶部栏

**作用：** 建立产品专业感。

**显示内容：**

- FactorX 品牌名
- 副标题：国际事件驱动股票影响评估
- 数据模式：Demo / 实时
- 最后分析时间

**状态标签：**

| 标签 | 含义 |
|------|------|
| 模拟数据 | 当前使用 mock 数据 |
| 规则模型 | 当前使用规则版评分 |
| ReLU 因子引擎 | ReLU 模块已激活 |

**视觉建议：**

- 背景深色或浅灰金融终端风
- 不要做花哨 landing page
- 更像 Bloomberg / Wind / TradingView 的工作台

---

### 4.2 NewsInputPanel 新闻输入区

这是 MVP 的入口。

**字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| headline | string | 新闻标题 |
| source | string | 新闻来源 |
| body | string | 新闻正文 |

**按钮：**

| 按钮 | 功能 |
|------|------|
| AI 分析 | 触发分析流程 |
| 载入示例 | 填充示例新闻 |
| 清空 | 清空输入 |

**输入示例：**

> Saudi Arabia announces $10B solar storage project involving Tesla suppliers

**组件状态：**

```typescript
type AnalysisRequest = {
  headline: string;
  source: string;
  body: string;
};
```

**交互逻辑：**

```
用户输入新闻
  → 点击 AI 分析
  → 按钮进入 loading
  → 调用 POST /api/analyze
  → 成功后更新页面结果
  → 失败后显示错误提示
```

---

### 4.3 EventExtractionPanel 事件提取区

展示 AI/规则抽取后的结构化事件。

**字段展示：**

| 字段 | 示例值 |
|------|--------|
| 事件类型 | 国际项目 |
| 行业 | 新能源 |
| 国家/地区 | Saudi Arabia |
| 项目金额 | $10.00B |
| 相关公司 | Tesla、Enphase、SolarEdge |
| 新闻源可信度 | 86% |

**数据结构：**

```typescript
type ExtractedEvent = {
  eventType: string;
  sector: string;
  country?: string;
  projectAmountUsd?: number;
  companies: string[];
  source: string;
  sourceCredibility: number;
};
```

**显示原则：**

- 能识别就展示具体值
- 不能识别就显示 **"待确认"**
- **不要编造公司和金额**

---

### 4.4 StockImpactPanel 股票影响评估区

这是用户最关注的区域。

**展示形式：** 表格或紧凑列表

| 股票 | 公司 | 关系 | 方向 | 概率 | 预估影响 | 周期 |
|------|------|------|------|------|----------|------|
| TSLA | Tesla | 直接相关 | 利好 | 74% | +2.0%~+5.5% | 3-10天 |
| ENPH | Enphase | 产业链相关 | 利好 | 63% | +1.1%~+3.8% | 3-10天 |
| NVDA | Nvidia | 行业相关 | 利好 | 58% | +0.8%~+2.9% | 3-10天 |

**数据结构：**

```typescript
type StockImpact = {
  symbol: string;
  company: string;
  relation: "直接相关" | "产业链相关" | "行业相关";
  direction: "利好" | "利空" | "中性";
  probability: number;
  estimatedMove: string;
  horizon: string;
  relevance: number;
};
```

**视觉规则：**

| 方向 | 标签颜色 |
|------|----------|
| 利好 | 绿色 |
| 利空 | 红色 |
| 中性 | 灰色 |

- 概率越高，进度条越长
- 用户点击某只股票后：
  - 下方 ReLU 曲线切换为该股票
  - 因子解释切换为该股票

---

## 五、核心模块：ReluMomentumChart

> 这是 FactorX 的独家功能，必须做得突出。

### 5.1 三条曲线

| 曲线 | 含义 |
|------|------|
| 原始净值曲线 | 真实价格波动 |
| 传统累计收益曲线 | 涨跌都计入 |
| ReLU 正向动量曲线 | 只累计超过阈值的正收益 |

### 5.2 金融含义

- **原始净值**：真实价格波动
- **传统累计收益**：涨跌都计入
- **ReLU 动量**：只累计超过阈值的正收益

### 5.3 核心公式

```
日收益率：    r_t = ln(P_t / P_{t-1})
ReLU 截断：   relu_t = max(0, r_t - threshold)
ReLU 累计：   M_t = Σ relu_t
```

### 5.4 图形效果

| 场景 | 曲线表现 |
|------|----------|
| 上涨日 | ReLU 曲线向上形成台阶 |
| 下跌/横盘日 | ReLU 曲线保持水平平台 |

### 5.5 关键指标展示

| 指标 | 示例值 | 说明 |
|------|--------|------|
| ReLU 动量斜率 | 0.82% / 日 | 正向动量密度 |
| 正收益密度 | 68% | 超过阈值的天数占比 |
| 平台天数 | 4 天 | 下跌或横盘天数 |
| 动量纯度 | 0.74 | 综合动量质量 |

### 5.6 数据结构

```typescript
type ReluMomentumPoint = {
  day: number;
  price: number;
  returnPct: number;
  cumulativeReturn: number;
  reluReturn: number;
  reluMomentum: number;
};
```

> 第一版可以先用模拟行情数据，后续接真实行情 API。

---

## 六、FactorActivationPanel 因子激活区

展示 ReLU 因子提取结果。

### 6.1 六个因子

| 序号 | 因子名称 |
|------|----------|
| 1 | 国际项目规模因子 |
| 2 | 新闻源可信度因子 |
| 3 | 股票关联度因子 |
| 4 | 行业景气因子 |
| 5 | 市场确认因子 |
| 6 | ReLU 动量因子 |

### 6.2 每个因子显示

| 字段 | 说明 |
|------|------|
| 因子名称 | 因子中文名 |
| Raw Score | 原始分数 |
| Threshold | 激活阈值 |
| Activation | 激活后分数 |
| 原因说明 | 自然语言解释 |

### 6.3 示例

```
国际项目规模因子
  Raw Score:   0.88
  Threshold:   0.35
  Activation:  0.82
  原因：项目金额较大，可能带来订单和产业链需求。
```

### 6.4 数据结构

```typescript
type ReluFactor = {
  name: string;
  rawScore: number;
  threshold: number;
  activation: number;
  reason: string;
};
```

### 6.5 激活公式

```
activation = max(0, rawScore - threshold) / (1 - threshold)
```

### 6.6 展示方式（进度条）

```
国际项目规模因子   ████████████  0.82
股票关联度因子     █████████████ 0.85
市场确认因子       ███           0.18
```

---

## 七、ExplanationPanel AI 解释区

这个区域负责把模型结果讲成人话。

### 7.1 示例解释

> 系统识别该新闻属于新能源国际项目事件，项目金额约 100 亿美元，相关公司与储能、太阳能供应链存在较高关联。TSLA 的股票关联度较高，新闻源可信度较强，ReLU 动量曲线呈现连续台阶上升，因此系统判断该事件对 TSLA 短期偏利好。

### 7.2 解释必须包含

- [ ] 为什么匹配这只股票
- [ ] 为什么是利好/利空
- [ ] 为什么概率是这个水平
- [ ] 哪些因子已经激活
- [ ] 哪些风险尚未确认

---

## 八、RiskNotePanel 风险提示区

> 金融产品必须有风险提示，不能只展示"预测"。

### 8.1 风险提示示例

> 市场确认因子尚未完全激活，当前判断主要来自事件文本和产业链匹配。若后续成交量没有放大，或股价已提前反应，实际影响可能低于模型估计。该结果不构成投资建议。

### 8.2 风险标签

| 标签 | 含义 |
|------|------|
| 市场未确认 | 成交量/价格尚未确认 |
| 可能已定价 | 股价可能已提前反应 |
| 消息源待验证 | 新闻来源可信度不足 |
| 行业联动不足 | 行业 ETF 未同步表现 |
| 成交量不足 | 放量确认缺失 |

---

## 九、前端状态设计

### 9.1 主页面状态

```typescript
const [request, setRequest] = useState<AnalysisRequest>({
  headline: "",
  source: "",
  body: "",
});

const [analysis, setAnalysis] = useState<AnalysisResponse | null>(null);
const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);
const [isAnalyzing, setIsAnalyzing] = useState(false);
const [error, setError] = useState<string | null>(null);
```

### 9.2 完整响应类型

```typescript
type AnalysisResponse = {
  analyzedAt: string;
  event: ExtractedEvent;
  stocks: StockImpact[];
  reluMomentum: ReluMomentumPoint[];
  reluFactors: ReluFactor[];
  explanation: string;
  riskNote: string;
};
```

### 9.3 状态流转

```
empty（初始无数据）
  ↓ 用户输入 + 点击分析
loading（isAnalyzing = true）
  ↓ 接口返回
success（analysis 有值）或 error（error 有值）
  ↓ 用户点击股票
selectedSymbol 切换，曲线和因子联动更新
```

---

## 十、接口调用设计

### 10.1 analysisApi.ts

```typescript
import axios from "axios";

export async function analyzeNews(payload: AnalysisRequest) {
  const res = await axios.post<AnalysisResponse>(
    "http://localhost:8080/api/analyze",
    payload
  );

  return res.data;
}
```

### 10.2 点击分析处理

```typescript
async function handleAnalyze() {
  setIsAnalyzing(true);
  setError(null);

  try {
    const result = await analyzeNews(request);
    setAnalysis(result);
    setSelectedSymbol(result.stocks[0]?.symbol ?? null);
  } catch {
    setError("分析失败，请检查后端服务是否启动。");
  } finally {
    setIsAnalyzing(false);
  }
}
```

### 10.3 第一版降级方案

- 后端未启动时，使用 `data/demoAnalysis.ts` 中的 mock 数据
- 通过环境变量或开关控制使用真实 API 还是 mock 数据

---

## 十一、页面视觉风格

### 11.1 风格定位

> **金融终端 + AI 分析台**

### 11.2 配色方案

| 用途 | 颜色 |
|------|------|
| 背景 | 深灰 / 极浅灰 |
| 主色 | 蓝绿色或青色 |
| 利好 | 绿色 |
| 利空 | 红色 |
| 中性 | 灰色 |
| 高亮 | 金色或蓝色 |

### 11.3 设计原则

- 不要做成营销网站，不要大 hero
- 应该像一个专业工具：
  - **密集但清晰**
  - **信息优先**
  - **曲线突出**
  - **解释可追溯**
  - **风险提示明确**

---

## 十二、第一版验收标准

React 原型做到以下 10 项即算完成：

- [ ] 1. 可以输入国际新闻标题、来源、正文
- [ ] 2. 可以点击 AI 分析
- [ ] 3. 可以展示事件类型、行业、金额、公司
- [ ] 4. 可以展示相关股票列表
- [ ] 5. 可以展示方向、概率、预估涨跌幅
- [ ] 6. 可以展示 ReLU 非对称动量曲线
- [ ] 7. 可以展示因子激活条
- [ ] 8. 可以展示 AI 解释
- [ ] 9. 可以展示风险提示
- [ ] 10. 有 loading、error、empty 三种状态

### 核心目标

第一版的页面核心不是"漂亮"，而是让用户一眼看懂：

| 问题 | 页面回答 |
|------|----------|
| 这条国际新闻影响谁？ | 股票影响评估区 |
| 为什么影响它？ | 事件提取 + 因子激活 + AI 解释 |
| 影响方向是什么？ | 利好/利空标签 |
| 概率有多高？ | 概率进度条 |
| ReLU 曲线是否支持这个判断？ | ReLU 动量曲线 + 动量纯度指标 |
| 风险在哪里？ | 风险提示区 + 风险标签 |

---

*文档结束*
