# FactorX 技术架构设计文档

> ReLU 非对称动量因子 + 国际新闻事件驱动股票影响评估平台

---

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | FactorX |
| 文档类型 | 技术架构设计 |
| 版本 | v1.0 (MVP) |
| 核心定位 | 金融工程型 AI 平台 |

---

## 目录

1. [核心定位](#一核心定位)
2. [整体系统架构](#二整体系统架构)
3. [数据流设计](#三数据流设计)
4. [推荐技术栈](#四推荐技术栈)
5. [前端页面架构](#五前端页面架构)
6. [后端模块设计](#六后端模块设计)
7. [核心算法设计](#七核心算法设计)
8. [股票影响评分](#八股票影响评分)
9. [数据库设计](#九数据库设计)
10. [实现顺序](#十实现顺序)
11. [第一版验收标准](#十一第一版验收标准)

---

## 一、核心定位

FactorX 不是普通财经新闻平台，而是一个**金融工程型 AI 平台**。

平台实时收集国际新闻、公告、项目和市场数据，通过事件抽取、股票映射、ReLU 非对称动量因子和多因子评分模型，评估新闻事件对股票的影响方向、概率和预估涨跌幅。

### 核心功能链路

```
国际新闻输入
    ↓
事件抽取
    ↓
公司/行业/股票匹配
    ↓
ReLU 非对称动量因子构建
    ↓
股票影响评分
    ↓
曲线展示与解释
```

---

## 二、整体系统架构

### 2.1 第一版架构（前后端分离单体）

第一版采用前后端分离架构，后端使用 Spring Boot 单体，不拆微服务，优先跑通闭环。

```
┌─────────────────────────────────────────────────────────┐
│                    React 前端                            │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│            Spring Boot API 网关 / 业务后端               │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                     分析引擎层                           │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ 新闻事件抽取  │  │  股票映射     │  │ ReLU 因子引擎  │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
│  ┌──────────────┐  ┌──────────────┐                     │
│  │ 影响评分引擎  │  │ AI 解释引擎   │                     │
│  └──────────────┘  └──────────────┘                     │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│                       数据层                             │
│  ┌──────────┐  ┌───────┐  ┌──────────┐  ┌───────────┐  │
│  │PostgreSQL│  │ Redis │  │ 行情数据源 │  │ 新闻数据源 │  │
│  └──────────┘  └───────┘  └──────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 2.2 后期可升级架构

```
Spring Boot 主后端
        ↓
Python Model Service
        ↓
XGBoost / PyTorch / 回测系统
```

> **架构原则**：第一版不要一开始拆微服务，先用 Spring Boot 单体后端跑通闭环。

---

## 三、数据流设计

### 3.1 完整数据流

```
1. 用户输入国际新闻 / 系统抓取新闻
        ↓
2. 新闻清洗、去重、语言识别、摘要
        ↓
3. AI/规则抽取事件结构
        ↓
4. 匹配公司、行业、产业链、股票代码
        ↓
5. 拉取股票近期收益率、成交量、波动率
        ↓
6. 构建 ReLU 非对称动量曲线
        ↓
7. 计算项目因子、新闻因子、股票关联因子、市场确认因子
        ↓
8. 输出方向、概率、涨跌幅区间、影响周期
        ↓
9. React 前端展示曲线、因子条、解释和风险提示
```

### 3.2 MVP 简化数据流

```
手动输入新闻
    → 规则抽取事件
    → 静态股票关键词匹配
    → 模拟/本地行情数据
    → ReLU 曲线展示
    → 影响评分
```

---

## 四、推荐技术栈

### 4.1 前端

| 技术 | 用途 |
|------|------|
| React | UI 框架 |
| TypeScript | 类型安全 |
| Vite | 构建工具 |
| Tailwind CSS | 样式框架 |
| ECharts / Recharts / SVG 自绘 | 曲线可视化 |
| Axios | HTTP 客户端 |
| React Query | 数据请求与缓存 |

### 4.2 后端

| 技术 | 用途 |
|------|------|
| Java 17 | 运行环境 |
| Spring Boot 3 | 应用框架 |
| Spring Web | REST API |
| Spring Validation | 参数校验 |
| Spring Data JPA | 数据访问 |
| PostgreSQL | 关系型数据库 |
| Redis | 缓存 |
| Quartz / Spring Scheduler | 定时任务 |

### 4.3 AI 与量化

| 技术 | 用途 | 阶段 |
|------|------|------|
| OpenAI API | 新闻事件抽取、解释生成 | MVP |
| Java 规则引擎 | MVP 评分 | MVP |
| Python FastAPI | 模型服务 | 后期 |
| XGBoost / LightGBM | 影响概率模型 | 后期 |
| PyTorch | 神经网络 ReLU 因子模型 | 后期 |

### 4.4 数据源

| 类别 | 推荐来源 |
|------|----------|
| 新闻 | GDELT / NewsAPI / Finnhub / RSS / PR Newswire |
| 行情 | Yahoo Finance / Finnhub / Alpha Vantage / Polygon |
| 公告 | SEC / 公司官网 / 交易所公告 |
| 产业链 | 自建公司-行业-供应链关系表 |

---

## 五、前端页面架构

### 5.1 主工作台布局

第一版做一个主工作台页面 **FactorX Dashboard**：

```
┌──────────────┬──────────────────────┬──────────────────┐
│              │                      │                  │
│  左侧        │  中间                │  右侧            │
│  国际新闻流   │  事件抽取结果        │  股票影响评估     │
│              │  + 新闻原文          │                  │
│              │                      │                  │
├──────────────┴──────────────────────┴──────────────────┤
│                                                        │
│  底部：ReLU 非对称动量曲线 + 因子解释                    │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 5.2 核心组件

| 组件名 | 功能 |
|--------|------|
| `NewsInputPanel` | 新闻输入 |
| `NewsFeed` | 国际新闻流 |
| `EventExtractionPanel` | 事件结构化结果 |
| `StockImpactPanel` | 股票影响预测 |
| `ReluMomentumChart` | ReLU 非对称动量曲线 |
| `FactorActivationPanel` | 因子激活条 |
| `ExplanationPanel` | AI 解释与风险提示 |

### 5.3 前端核心数据结构

```typescript
type AnalysisResponse = {
  event: ExtractedEvent;
  stocks: StockImpact[];
  reluMomentum: ReluMomentumPoint[];
  reluFactors: ReluFactor[];
  explanation: string;
  riskNote: string;
};

type ReluMomentumPoint = {
  day: number;
  returnPct: number;
  cumulativeReturn: number;
  reluReturn: number;
  reluMomentum: number;
};
```

---

## 六、后端模块设计

### 6.1 包结构

```
controller/
  ├── AnalysisController
  ├── NewsController
  └── StockController

service/
  ├── NewsAnalysisService
  ├── EventExtractorService
  ├── StockMatcherService
  ├── ReluFactorService
  ├── ScoringService
  └── ExplanationService

model/
  ├── dto/
  ├── entity/
  └── enums/

repository/
  ├── NewsArticleRepository
  ├── EventRepository
  └── StockImpactRepository
```

### 6.2 核心 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/analyze` | 新闻分析主入口 |
| GET | `/api/demo` | Demo 数据 |
| GET | `/api/news` | 新闻列表 |
| GET | `/api/events` | 事件列表 |
| GET | `/api/stocks/{symbol}/impacts` | 股票影响记录 |
| GET | `/api/stocks/{symbol}/relu-momentum` | 股票 ReLU 动量 |

### 6.3 POST /api/analyze

**请求体：**

```json
{
  "headline": "Saudi Arabia announces $10B solar storage project involving Tesla suppliers",
  "source": "Reuters",
  "body": "..."
}
```

**响应体：**

```json
{
  "event": {
    "eventType": "国际项目",
    "sector": "新能源",
    "projectAmountUsd": 10000000000,
    "companies": ["Tesla"],
    "sourceCredibility": 0.86
  },
  "stocks": [
    {
      "symbol": "TSLA",
      "company": "Tesla",
      "direction": "利好",
      "probability": 74,
      "estimatedMove": "+2.0% ~ +5.5%",
      "horizon": "3-10个交易日"
    }
  ],
  "reluFactors": [],
  "reluMomentum": [],
  "explanation": "...",
  "riskNote": "..."
}
```

---

## 七、核心算法设计

核心算法分两类：**新闻事件影响因子** 和 **股票 ReLU 非对称动量因子**。

### 7.1 新闻事件因子

#### 事件分数

```
eventScore =
    0.30 * projectScaleScore
  + 0.20 * sourceCredibilityScore
  + 0.20 * companyRelevanceScore
  + 0.15 * sectorHeatScore
  + 0.15 * marketConfirmationScore
```

#### 事件方向判定

**利好关键词：**
`announce`, `wins`, `contract`, `investment`, `subsidy`, `partnership`, `expansion`

**利空关键词：**
`ban`, `sanction`, `lawsuit`, `delay`, `cancel`, `probe`, `miss`, `recall`

#### 项目金额分数

```
projectScaleScore = min(1, 0.25 + log10(1 + projectAmountBillion) * 0.48)
```

### 7.2 ReLU 非对称动量因子

#### 日收益率

```
r_t = ln(P_t / P_{t-1})
```

#### ReLU 截断收益

```
relu_t = max(0, r_t - threshold)
```

#### ReLU 累计动量

```
M_t = Σ max(0, r_i - threshold)
```

#### 金融含义

| 场景 | 表现 |
|------|------|
| 负收益 / 横盘 | 归零，不进入正向因子 |
| 正收益超过阈值 | 计入有效动量 |
| 曲线斜率越大 | 正向动量密度越高 |
| 平台越长 | 有效上涨信号越弱 |

#### 核心指标

```
reluSlope = M_t / lookbackDays

positiveDensity = count(r_t > threshold) / lookbackDays

plateauRatio = count(r_t <= threshold) / lookbackDays

momentumPurity = reluSlope * positiveDensity * (1 - plateauRatio)
```

#### 产品化命名

| 指标 | 产品名称 |
|------|----------|
| momentumPurity | 阿尔法纯度 (Alpha Purity) |
| positiveDensity | 正向动量密度 (Positive Momentum Density) |
| plateauRatio | 平台风险 (Plateau Risk) |

---

## 八、股票影响评分

### 8.1 最终评分公式

最终股票评分融合新闻事件和 ReLU 动量：

```
finalImpactScore =
    0.45 * eventScore
  + 0.30 * stockRelevanceScore
  + 0.25 * reluMomentumScore
```

### 8.2 概率转换

```
probability = 45 + finalImpactScore * 40
```

### 8.3 涨跌幅区间

```
lowMove = 0.5 + finalImpactScore * 2
highMove = lowMove + 1.5 + finalImpactScore * 3
```

### 8.4 方向判定矩阵

| 事件方向 | ReLU 动量 | 结论 |
|----------|-----------|------|
| 利好 | 强 | 高概率利好 |
| 利好 | 弱 | 利好但需确认 |
| 利空 | 弱 | 利空概率上升 |
| 利空 | 强 | 风险提示：趋势与新闻冲突 |

---

## 九、数据库设计

### 9.1 news_articles（新闻文章）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR | 标题 |
| source | VARCHAR | 来源 |
| url | VARCHAR | 链接 |
| body | TEXT | 正文 |
| language | VARCHAR | 语言 |
| published_at | TIMESTAMP | 发布时间 |
| hash | VARCHAR | 去重哈希 |
| created_at | TIMESTAMP | 创建时间 |

### 9.2 events（事件）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| news_id | BIGINT | 关联新闻 |
| event_type | VARCHAR | 事件类型 |
| sector | VARCHAR | 行业 |
| country | VARCHAR | 国家 |
| project_amount_usd | DECIMAL | 项目金额(美元) |
| source_credibility | DECIMAL | 来源可信度 |
| summary | TEXT | 摘要 |

### 9.3 event_companies（事件关联公司）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| event_id | BIGINT | 关联事件 |
| company_name | VARCHAR | 公司名称 |
| symbol | VARCHAR | 股票代码 |
| relation_type | VARCHAR | 关系类型 |
| relevance_score | DECIMAL | 关联度分数 |

### 9.4 stock_prices（股票行情）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| symbol | VARCHAR | 股票代码 |
| trade_date | DATE | 交易日 |
| close_price | DECIMAL | 收盘价 |
| volume | BIGINT | 成交量 |
| return_pct | DECIMAL | 收益率 |

### 9.5 relu_momentum_points（ReLU 动量点）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| symbol | VARCHAR | 股票代码 |
| trade_date | DATE | 交易日 |
| return_pct | DECIMAL | 收益率 |
| relu_return | DECIMAL | ReLU 截断收益 |
| relu_momentum | DECIMAL | ReLU 累计动量 |
| threshold | DECIMAL | 阈值 |

### 9.6 relu_factors（ReLU 因子）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| event_id | BIGINT | 关联事件 |
| symbol | VARCHAR | 股票代码 |
| factor_name | VARCHAR | 因子名称 |
| raw_score | DECIMAL | 原始分数 |
| threshold | DECIMAL | 阈值 |
| activation | DECIMAL | 激活值 |
| reason | TEXT | 原因 |

### 9.7 stock_impacts（股票影响评估）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| event_id | BIGINT | 关联事件 |
| symbol | VARCHAR | 股票代码 |
| direction | VARCHAR | 方向(利好/利空) |
| probability | DECIMAL | 概率 |
| estimated_low | DECIMAL | 预估涨跌幅下限 |
| estimated_high | DECIMAL | 预估涨跌幅上限 |
| horizon | VARCHAR | 影响周期 |
| explanation | TEXT | 解释 |

---

## 十、实现顺序

建议按以下顺序开发：

| 序号 | 阶段 | 内容 |
|------|------|------|
| 1 | 前端原型 | React 页面原型 |
| 2 | 核心 API | Spring Boot `/api/analyze` |
| 3 | 事件抽取 | 规则版新闻事件抽取 |
| 4 | 股票匹配 | 股票关键词匹配 |
| 5 | 因子引擎 | ReLU 非对称动量曲线模块 |
| 6 | 评分模型 | 因子激活与评分模型 |
| 7 | 数据存储 | PostgreSQL 存储 |
| 8 | 新闻接入 | 接入真实新闻源 |
| 9 | 行情接入 | 接入真实行情数据 |
| 10 | 模型优化 | 历史回测和模型训练 |

### 第一阶段 MVP 范围

```
输入新闻
  → 分析事件
  → 匹配股票
  → 展示 ReLU 曲线
  → 输出概率、涨跌幅、解释
```

---

## 十一、第一版验收标准

MVP 做到以下功能即算跑通闭环：

- [ ] 可以输入一条国际新闻
- [ ] 可以提取事件类型、行业、项目金额、公司
- [ ] 可以匹配相关股票
- [ ] 可以展示 ReLU 非对称动量曲线
- [ ] 可以展示 ReLU 激活因子
- [ ] 可以输出方向、概率、预估涨跌幅
- [ ] 可以解释为什么影响这只股票
- [ ] 可以提示风险和不确定性

### 核心理念

> **ReLU 曲线不是装饰图，而是核心金融工程模块。**
>
> - 新闻事件告诉我们：**可能影响谁**
> - ReLU 动量告诉我们：**这只股票是否已经具备正向动量结构**
> - 最终评分告诉我们：**这个影响有多大概率变成价格反应**

---

*文档结束*
