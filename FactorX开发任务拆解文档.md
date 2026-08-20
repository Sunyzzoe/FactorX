# FactorX 开发任务拆解文档

> 第一版 MVP 开发路线 · 10 个任务 · 三阶段里程碑

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | FactorX |
| 文档类型 | 开发任务拆解 |
| 版本 | v1.0 |
| 任务数量 | 10 个 |
| 阶段划分 | MVP / 数据化 / 模型化 |

---

## 目录

- [任务 1：React 页面原型](#任务-1react-页面原型)
- [任务 2：Spring Boot /api/analyze](#任务-2spring-boot-apianalyze)
- [任务 3：规则版新闻事件抽取](#任务-3规则版新闻事件抽取)
- [任务 4：股票关键词匹配](#任务-4股票关键词匹配)
- [任务 5：ReLU 非对称动量曲线模块](#任务-5relu-非对称动量曲线模块)
- [任务 6：因子激活与评分模型](#任务-6因子激活与评分模型)
- [任务 7：PostgreSQL 存储](#任务-7postgresql-存储)
- [任务 8：接入真实新闻源](#任务-8接入真实新闻源)
- [任务 9：接入真实行情数据](#任务-9接入真实行情数据)
- [任务 10：历史回测和模型训练](#任务-10历史回测和模型训练)
- [里程碑规划](#里程碑规划)
- [核心闭环](#核心闭环)

---

## 任务 1：React 页面原型

### 目标

先做出可演示的产品界面，让用户完成一次完整分析。

### 要做什么

搭建 FactorX Dashboard 主工作台，实现以下页面结构：

| 区域 | 内容 |
|------|------|
| 顶部 | FactorX 品牌、数据状态、最后更新时间 |
| 左侧 | 国际新闻输入 / 新闻流 |
| 中间 | 事件抽取结果 |
| 右侧 | 股票影响评估 |
| 底部 | ReLU 非对称动量曲线 + 因子解释 |

**核心组件：**

| 组件 | 功能 |
|------|------|
| `NewsInputPanel` | 输入国际新闻 |
| `EventExtractionPanel` | 展示事件类型、行业、金额、公司 |
| `StockImpactPanel` | 展示股票、方向、概率、涨跌幅 |
| `ReluMomentumChart` | 展示 ReLU 曲线 |
| `FactorActivationPanel` | 展示因子激活值 |
| `ExplanationPanel` | 展示 AI 解释和风险提示 |

### 实现思路

1. 使用 **React + TypeScript + Vite** 初始化项目
2. 图表先用 **Recharts** 或 **SVG 自绘**
3. 页面先接 **mock 数据**，确保 UI 完整可交互
4. 后续替换为 `/api/analyze` 返回的真实结果
5. 使用 Tailwind CSS 统一视觉风格

### 关键接口/数据

**Mock 数据结构：**

```typescript
type AnalysisResponse = {
  event: ExtractedEvent;
  stocks: StockImpact[];
  reluMomentum: ReluMomentumPoint[];
  reluFactors: ReluFactor[];
  explanation: string;
  riskNote: string;
};
```

**后续对接接口：** `POST /api/analyze`

### 验收标准

- [ ] 能输入新闻
- [ ] 能点击分析按钮触发分析流程
- [ ] 能显示事件抽取结果（类型、行业、金额、公司）
- [ ] 能显示股票影响（股票代码、方向、概率、涨跌幅）
- [ ] 能展示 ReLU 非对称动量曲线
- [ ] 能展示因子激活条
- [ ] 能展示 AI 解释和风险提示

---

## 任务 2：Spring Boot /api/analyze

### 目标

提供核心分析接口，作为前后端对接的主入口。

### 要做什么

实现 `POST /api/analyze` 接口，接收新闻输入，编排全流程分析，返回结构化结果。

### 实现思路

1. **AnalysisController** 接收 HTTP 请求
2. **NewsAnalysisService** 编排整体分析流程
3. 内部分别调用以下 Service：
   - `EventExtractorService` — 事件抽取
   - `StockMatcherService` — 股票匹配
   - `ReluFactorService` — ReLU 因子计算
   - `ScoringService` — 影响评分
   - `ExplanationService` — 解释生成
4. 第一版可以先不接数据库，直接在内存中计算并返回结果
5. 添加参数校验和统一异常处理

### 关键接口/数据

**请求：**

```json
POST /api/analyze
{
  "headline": "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
  "source": "Reuters",
  "body": "..."
}
```

**响应：**

```json
{
  "event": {},
  "stocks": [],
  "reluMomentum": [],
  "reluFactors": [],
  "explanation": "",
  "riskNote": ""
}
```

**类结构：**

```
controller/
  └── AnalysisController
service/
  ├── NewsAnalysisService (编排)
  ├── EventExtractorService
  ├── StockMatcherService
  ├── ReluFactorService
  ├── ScoringService
  └── ExplanationService
```

### 验收标准

- [ ] 前端能成功调用接口并获得 200 响应
- [ ] 接口能返回完整 JSON 结构（event / stocks / reluMomentum / reluFactors / explanation / riskNote）
- [ ] 错误输入（空标题、超长正文等）能返回清晰错误提示
- [ ] 接口响应时间在可接受范围内（MVP 阶段 < 3s）

---

## 任务 3：规则版新闻事件抽取

### 目标

从国际新闻中提取结构化事件信息，不依赖外部 AI API。

### 要做什么

从新闻标题和正文中提取以下字段：

| 字段 | 说明 |
|------|------|
| eventType | 事件类型 |
| sector | 行业 |
| country | 国家/地区 |
| projectAmountUsd | 项目金额（美元） |
| companies | 公司名称列表 |
| keywords | 关键词 |
| sourceCredibility | 新闻源可信度 |

### 实现思路

**金额提取（正则）：**

| 模式 | 示例 |
|------|------|
| `$10B` / `$10 billion` | 100 亿美元 |
| `USD 2.5 billion` | 25 亿美元 |
| `$500 million` | 5 亿美元 |

**行业映射（关键词）：**

| 关键词 | 行业 |
|--------|------|
| solar / battery / EV | 新能源 |
| GPU / AI chip / data center | AI 芯片 |
| semiconductor / foundry / lithography | 半导体 |

**事件类型判断（关键词）：**

| 关键词 | 事件类型 |
|--------|----------|
| project / factory / plant | 国际项目 |
| contract / order | 订单合同 |
| subsidy / policy / regulation | 政策监管 |
| ban / sanction / lawsuit | 风险事件 |

**来源可信度：** 内置主流媒体可信度评分表（Reuters 0.9, Bloomberg 0.88 等）。

### 关键接口/数据

**输出示例：**

```json
{
  "eventType": "国际项目",
  "sector": "新能源",
  "country": "Saudi Arabia",
  "projectAmountUsd": 10000000000,
  "companies": ["Tesla"],
  "sourceCredibility": 0.86
}
```

**Service 接口：**

```java
ExtractedEvent extract(String headline, String body, String source);
```

### 验收标准

- [ ] 能从标题和正文中提取金额（支持 B / million / billion 等单位）
- [ ] 能识别行业分类（新能源、AI 芯片、半导体等）
- [ ] 能判断事件类型（国际项目、订单合同、政策监管、风险事件）
- [ ] 能提取公司名称
- [ ] 无法识别时显示"待确认"，不编造数据
- [ ] 能输出新闻源可信度评分

---

## 任务 4：股票关键词匹配

### 目标

把新闻里的公司、行业、产业链映射到具体股票代码。

### 要做什么

建立静态股票词典，根据新闻内容匹配相关股票，并区分关联程度。

### 实现思路

**第一版静态股票词典：**

| 股票代码 | 关键词 |
|----------|--------|
| TSLA | Tesla, EV, battery, solar, energy storage |
| NVDA | Nvidia, GPU, AI chip, data center |
| AMD | GPU, AI chip, semiconductor |
| ASML | lithography, semiconductor equipment |
| TSM | foundry, chip manufacturing |
| ENPH | inverter, solar |

**匹配逻辑与关联度：**

| 匹配情况 | 关系类型 | relevance 范围 |
|----------|----------|----------------|
| 直接出现公司名 | 直接相关 | 0.85 - 0.95 |
| 命中供应链关键词 | 产业链相关 | 0.65 - 0.80 |
| 只命中行业关键词 | 行业相关 | 0.45 - 0.65 |

### 关键接口/数据

**输出示例：**

```json
{
  "symbol": "TSLA",
  "company": "Tesla",
  "relation": "直接相关",
  "relevance": 0.91
}
```

**Service 接口：**

```java
List<MatchedStock> match(ExtractedEvent event);
```

### 验收标准

- [ ] 输入一条新闻，至少能匹配出相关股票
- [ ] 能区分"直接相关"、"产业链相关"、"行业相关"三种关系
- [ ] 能输出每只股票的关联度分数（relevance）
- [ ] 多只股票时按关联度降序排列

---

## 任务 5：ReLU 非对称动量曲线模块

### 目标

实现核心金融工程功能——ReLU 非对称动量因子计算与可视化。

### 要做什么

1. 根据股票价格序列计算日收益率、ReLU 截断收益、ReLU 累计动量
2. 输出三条曲线：原始净值曲线、传统累计收益曲线、ReLU 正向动量曲线
3. 计算核心指标：斜率、正收益密度、平台占比、动量纯度
4. 支持调节 threshold 参数

### 实现思路

**核心公式：**

```
日收益率：    r_t = ln(P_t / P_{t-1})
ReLU 截断：   relu_t = max(0, r_t - threshold)
ReLU 累计：   M_t = Σ max(0, r_i - threshold)
```

**曲线含义：**

| 曲线特征 | 金融含义 |
|----------|----------|
| 上升台阶 | 正收益被捕捉 |
| 水平平台 | 下跌或低效收益被截断 |
| 斜率越大 | 正向动量密度越高 |
| 平台越长 | 有效上涨信号越弱 |

**第一版**：如果还没有真实行情，用模拟价格序列生成曲线。后续接真实行情后，输入 `closePrice[]` 自动计算。

### 关键接口/数据

**核心指标：**

```
reluSlope = M_t / lookbackDays
positiveDensity = count(r_t > threshold) / lookbackDays
plateauRatio = count(r_t <= threshold) / lookbackDays
momentumPurity = reluSlope * positiveDensity * (1 - plateauRatio)
```

**曲线点数据结构：**

```typescript
type ReluMomentumPoint = {
  day: number;
  returnPct: number;
  cumulativeReturn: number;
  reluReturn: number;
  reluMomentum: number;
};
```

**Service 接口：**

```java
ReluResult calculate(List<BigDecimal> closePrices, BigDecimal threshold);
```

### 验收标准

- [ ] 能展示 ReLU 台阶曲线（与传统累计收益曲线对比）
- [ ] 能调节 threshold 参数并实时更新曲线
- [ ] 能输出 reluSlope（斜率）
- [ ] 能输出 positiveDensity（正收益密度）
- [ ] 能输出 plateauRatio / 平台天数
- [ ] 能输出 momentumPurity（动量纯度）

---

## 任务 6：因子激活与评分模型

### 目标

把新闻事件和股票动量转成影响概率、方向和预估涨跌幅。

### 要做什么

1. 构建第一版因子体系（6 个因子）
2. 实现 ReLU 激活函数
3. 计算综合评分
4. 转换为概率和涨跌幅区间
5. 判定影响方向

### 实现思路

**第一版因子：**

| 因子 | 权重 | 说明 |
|------|------|------|
| 国际项目规模因子 | - | 基于项目金额 |
| 新闻源可信度因子 | - | 基于来源可信度 |
| 股票关联度因子 | - | 基于匹配 relevance |
| 行业景气因子 | - | 基于行业热度 |
| 市场确认因子 | - | 基于量价确认 |
| ReLU 动量因子 | - | 基于 momentumPurity |

**ReLU 激活公式：**

```
activation = max(0, rawScore - threshold) / (1 - threshold)
```

**综合评分：**

```
finalImpactScore =
    0.30 * eventScore
  + 0.25 * stockRelevanceScore
  + 0.20 * reluMomentumScore
  + 0.15 * sourceCredibilityScore
  + 0.10 * marketConfirmationScore
```

**概率转换：**

```
probability = 45 + finalImpactScore * 40
```

**涨跌幅估算：**

```
lowMove = 0.5 + finalImpactScore * 2
highMove = lowMove + 1.5 + finalImpactScore * 3
```

### 关键接口/数据

**输出结构：**

```json
{
  "symbol": "TSLA",
  "direction": "利好",
  "probability": 74,
  "estimatedLow": 2.0,
  "estimatedHigh": 5.5,
  "horizon": "3-10个交易日",
  "factors": [
    { "name": "项目规模", "rawScore": 0.82, "activation": 0.71 },
    { "name": "ReLU动量", "rawScore": 0.65, "activation": 0.54 }
  ]
}
```

**Service 接口：**

```java
List<StockImpact> score(ExtractedEvent event, List<MatchedStock> stocks, ReluResult relu);
```

### 验收标准

- [ ] 每只股票都有方向（利好/利空）、概率、涨跌幅区间、影响周期
- [ ] 每个评分都能追溯到具体因子（可解释性）
- [ ] 能展示每个因子的 rawScore 和 activation
- [ ] 概率范围在合理区间（45%-85%）
- [ ] 涨跌幅区间与评分正相关

---

## 任务 7：PostgreSQL 存储

### 目标

保存分析历史，为后续回测和模型训练做准备。

### 要做什么

1. 建立核心数据表
2. 每次分析完成后持久化全部结果
3. 支持按新闻、股票、事件查询历史记录

### 实现思路

1. 使用 **Spring Data JPA** 建实体类
2. 每次分析保存以下数据：
   - 原始新闻
   - 事件抽取结果
   - 匹配股票列表
   - 因子激活值
   - 影响评分结果
   - ReLU 曲线点
3. **最重要的是保留历史快照**——以后要验证：
   - 当时模型怎么判断
   - 后面股价实际怎么走
   - 模型有没有判断错

### 关键接口/数据

**核心表：**

| 表名 | 用途 |
|------|------|
| `news_articles` | 原始新闻 |
| `events` | 事件抽取结果 |
| `event_companies` | 事件关联公司 |
| `stock_impacts` | 股票影响评估 |
| `relu_factors` | ReLU 因子激活值 |
| `relu_momentum_points` | ReLU 曲线点 |
| `stock_prices` | 股票行情 |

**查询接口：**

```
GET /api/news              — 新闻列表
GET /api/events            — 事件列表
GET /api/stocks/{symbol}/impacts       — 某股票影响历史
GET /api/stocks/{symbol}/relu-momentum — 某股票 ReLU 历史
```

### 验收标准

- [ ] 每次分析结果能完整入库（新闻 + 事件 + 股票 + 因子 + 曲线）
- [ ] 能按新闻查询关联的事件和影响评估
- [ ] 能按股票代码查询历史影响记录
- [ ] 能按事件类型筛选历史
- [ ] 重复新闻通过 hash 去重，不重复入库

---

## 任务 8：接入真实新闻源

### 目标

从手动输入升级为实时收集国际新闻。

### 要做什么

1. 接入多个新闻数据源
2. 定时抓取、清洗、去重、入库
3. 自动触发分析流程
4. 新闻自动进入前端新闻流

### 实现思路

**接入优先级：**

| 优先级 | 数据源 | 说明 |
|--------|--------|------|
| 1 | RSS / GDELT | 免费、覆盖广 |
| 2 | Finnhub 财经新闻 | 财经聚焦 |
| 3 | NewsAPI | 通用新闻 |
| 4 | PR Newswire / GlobeNewswire | 官方公告 |
| 5 | SEC / 交易所公告 | 权威披露 |

**抓取流程：**

```
Spring Scheduler 每 5 分钟抓取
  → 清洗标题和正文
  → hash 去重
  → 入库
  → 自动调用分析流程
```

**去重方式：**

```
hash = sha256(title + source + publishedAt)
```

**需要新增字段：** `source`, `url`, `publishedAt`, `language`, `region`, `sectorHint`

### 关键接口/数据

**定时任务：**

```java
@Scheduled(fixedRate = 300000) // 每5分钟
public void fetchNews() { ... }
```

**新闻实体新增字段：**

```java
private String source;
private String url;
private LocalDateTime publishedAt;
private String language;
private String region;
private String sectorHint;
private String hash; // 去重
```

### 验收标准

- [ ] 系统能自动拉取新闻（无需手动输入）
- [ ] 重复新闻不会反复分析（hash 去重生效）
- [ ] 新闻能自动进入前端新闻流展示
- [ ] 新新闻入库后自动触发分析流程
- [ ] 能区分新闻来源和发布时间

---

## 任务 9：接入真实行情数据

### 目标

用市场反应确认新闻影响，不只看文本。

### 要做什么

1. 接入真实行情数据源
2. 获取收盘价、日收益率、成交量、波动率等数据
3. 实现市场确认规则
4. ReLU 曲线由真实行情生成

### 实现思路

**需要数据：**

| 数据 | 用途 |
|------|------|
| 收盘价 | 计算收益率和 ReLU |
| 日收益率 | 动量计算 |
| 成交量 | 市场确认 |
| 20 日均量 | 放量判断基准 |
| 波动率 | 风险评估 |
| 行业 ETF 表现 | 行业确认 |

**市场确认规则：**

| 条件 | 影响 |
|------|------|
| 成交量 > 2 × 20 日均量 | 市场确认增强 |
| 股价方向与新闻方向一致 | 确认增强 |
| 行业 ETF 同步上涨 | 行业确认增强 |
| 股价反向运动 | 风险提示增强 |

**行情接入源：** Yahoo Finance / Finnhub / Alpha Vantage / Polygon

**与 ReLU 模块结合：**

```
股票价格序列
  → 日收益率
  → ReLU 截断收益
  → ReLU 累计动量
  → reluMomentumScore
```

### 关键接口/数据

**行情 Service 接口：**

```java
StockQuote getQuote(String symbol);
List<StockPrice> getHistory(String symbol, int days);
MarketConfirmation checkConfirmation(String symbol, String direction);
```

**stock_prices 表：**

| 字段 | 说明 |
|------|------|
| symbol | 股票代码 |
| trade_date | 交易日 |
| close_price | 收盘价 |
| volume | 成交量 |
| return_pct | 日收益率 |

### 验收标准

- [ ] 股票影响评分能使用真实价格和成交量数据
- [ ] ReLU 曲线由真实行情生成（非模拟数据）
- [ ] 能计算 20 日均量并判断放量
- [ ] 市场确认因子能根据量价数据调整评分
- [ ] 股价反向运动时能给出风险提示
- [ ] 行情数据能定时更新并入库

---

## 任务 10：历史回测和模型训练

### 目标

验证模型是否有效，并逐步从规则模型升级为机器学习模型。

### 要做什么

1. 构建历史事件回测框架
2. 标注训练标签（未来 1/3/5/10 日收益）
3. 训练机器学习模型
4. 输出评估指标
5. 根据回测结果调整因子权重

### 实现思路

**回测流程：**

```
历史新闻事件
  → 当时提取因子
  → 匹配股票
  → 观察未来 1/3/5/10 日收益
  → 计算命中率和误差
```

**训练标签：**

| 标签 | 说明 |
|------|------|
| `label_1d_up` | 1 日后是否上涨 |
| `label_3d_up` | 3 日后是否上涨 |
| `label_5d_return` | 5 日收益率 |
| `label_10d_return` | 10 日收益率 |

**模型选择：**

| 模型 | 用途 |
|------|------|
| Logistic Regression | 上涨概率（基线） |
| XGBoost / LightGBM | 影响概率和因子重要性 |
| Random Forest | 稳健基线模型 |
| PyTorch MLP + ReLU | 高级非线性因子模型 |

**评估指标：** 方向准确率、AUC、平均收益、最大回撤、胜率、盈亏比、按行业分组表现

**实现架构：**

```
Spring Boot —— 业务、API、入库
     ↓
Python FastAPI —— 模型训练和预测
     ↓
PostgreSQL —— 训练数据
```

### 关键接口/数据

**回测 Service：**

```python
# Python FastAPI
POST /backtest
{
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "symbols": ["TSLA", "NVDA"]
}
```

**模型评估输出：**

```json
{
  "directionAccuracy": 0.62,
  "auc": 0.68,
  "avgReturn": 0.023,
  "maxDrawdown": -0.08,
  "winRate": 0.58,
  "profitLossRatio": 1.4,
  "bySector": {
    "新能源": { "accuracy": 0.65 },
    "半导体": { "accuracy": 0.59 }
  }
}
```

### 验收标准

- [ ] 能回测历史事件（指定时间范围和股票池）
- [ ] 能输出模型准确率（方向准确率、AUC）
- [ ] 能输出收益指标（平均收益、最大回撤、胜率、盈亏比）
- [ ] 能按行业分组评估表现
- [ ] 能根据回测结果调整因子权重
- [ ] 训练好的模型能通过 FastAPI 提供预测服务

---

## 里程碑规划

### 第一阶段 MVP（任务 1-6）

> 目标：跑通核心产品闭环

| 任务 | 内容 |
|------|------|
| 1 | React 原型 |
| 2 | /api/analyze 接口 |
| 3 | 规则抽取 |
| 4 | 股票匹配 |
| 5 | ReLU 曲线 |
| 6 | 评分模型 |

### 第二阶段 数据化（任务 7-9）

> 目标：从手动演示升级为实时数据驱动

| 任务 | 内容 |
|------|------|
| 7 | PostgreSQL 数据库存储 |
| 8 | 真实新闻源接入 |
| 9 | 真实行情数据接入 |

### 第三阶段 模型化（任务 10）

> 目标：从规则模型升级为机器学习模型

| 任务 | 内容 |
|------|------|
| 10 | 历史回测 + 机器学习 + 因子权重优化 |

---

## 核心闭环

最先做的闭环是：

```
输入国际新闻
  → 提取事件
  → 匹配股票
  → 生成 ReLU 曲线
  → 输出方向、概率、涨跌幅和解释
```

**这个闭环跑通后，FactorX 的核心产品形态就成立了。**

---

*文档结束*
