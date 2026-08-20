# FactorX PostgreSQL 存储详细讲解

## 1. 为什么需要 PostgreSQL

FactorX 不是只计算一次结果，而是要长期保存以下数据：

- 原始新闻
- 事件抽取结果
- 关联股票
- 评分因子
- ReLU 曲线
- 股票历史行情
- 后续实际涨跌结果

Redis 适合缓存，PostgreSQL 才适合作为长期历史数据库。后续模型回测和训练也需要从 PostgreSQL 读取完整历史数据。

---

## 2. 核心数据关系

原始设计的关系如下：

```
news_articles
      ↓
    events
      ↓
event_companies
      ↓
stock_impacts
      ↓
relu_factors
relu_momentum_points

stock_prices（独立）
```

含义说明：

1. 一篇新闻保存到 `news_articles`
2. 新闻经过事件抽取，保存到 `events`
3. 事件关联公司和股票，保存到 `event_companies`
4. 计算每只股票的影响结果，保存到 `stock_impacts`
5. 保存评分因子，保存到 `relu_factors`
6. 保存 ReLU 曲线，保存到 `relu_momentum_points`
7. 行情数据独立保存到 `stock_prices`

---

## 3. 核心表说明

### 3.1 news_articles

保存原始新闻。

| 字段 | 说明 |
|------|------|
| id | 主键 |
| title | 新闻标题 |
| source | 来源 |
| url | 链接 |
| body | 正文 |
| language | 语言 |
| published_at | 发布时间 |
| hash | 去重哈希 |
| created_at | 创建时间 |

**hash 用于新闻去重。** 建议使用标题、来源和正文计算 SHA-256：

```
hash = SHA256(source + title + body)
```

同一篇新闻重复抓取时，不应该重复保存。

---

### 3.2 events

保存新闻抽取出的事件。

| 字段 | 说明 |
|------|------|
| event_type | 事件类型 |
| sector | 行业板块 |
| country | 国家 |
| project_amount_usd | 项目金额（美元） |
| source_credibility | 来源可信度 |
| summary | 摘要 |

示例：

```json
{
  "eventType": "国际项目",
  "sector": "新能源",
  "country": "Saudi Arabia",
  "projectAmountUsd": 10000000000,
  "sourceCredibility": 0.86
}
```

---

### 3.3 event_companies

保存事件关联的公司和股票。

| 字段 | 说明 |
|------|------|
| company_name | 公司名称 |
| symbol | 股票代码 |
| relation_type | 关联类型 |
| relevance_score | 关联度评分 |

`relation_type` 可以是：

- 直接相关
- 产业链相关
- 行业相关

同一个事件可能关联多只股票，因此是一对多关系。

---

### 3.4 stock_impacts

保存最终影响评估。

| 字段 | 说明 |
|------|------|
| symbol | 股票代码 |
| direction | 方向（利好/利空） |
| probability | 概率 |
| estimated_low | 预估下限 |
| estimated_high | 预估上限 |
| horizon | 时间窗口 |
| explanation | 解释 |

示例：

```json
{
  "symbol": "TSLA",
  "direction": "利好",
  "probability": 74,
  "estimatedLow": 2.0,
  "estimatedHigh": 5.5,
  "horizon": "3-10个交易日"
}
```

**建议同时保存：**

- `final_impact_score` — 模型内部计算结果
- `relevance_score` — 关联度
- `risk_note` — 风险提示

因为 `probability` 是展示结果，而 `final_impact_score` 才是模型内部计算结果。只保存概率，未来无法复核模型过程。

---

### 3.5 relu_factors（建议命名为 factor_scores）

保存每个因子的计算结果。

| 字段 | 说明 |
|------|------|
| factor_name | 因子名称 |
| raw_score | 原始分数 |
| threshold | 阈值 |
| activation | 激活值 |
| reason | 原因 |

当前 FactorX 的因子不只有 ReLU，还包括：

- 国际项目规模
- 新闻源可信度
- 公司明确性
- 行业景气
- 市场确认
- 股票关联度
- ReLU 动量

因此实际实现时可以把表命名为 `factor_scores`，比 `relu_factors` 更准确。

---

### 3.6 relu_momentum_points

保存 ReLU 曲线上的每个点。

| 字段 | 说明 |
|------|------|
| symbol | 股票代码 |
| trade_date | 交易日 |
| return_pct | 日收益率 |
| relu_return | ReLU 收益 |
| relu_momentum | 累计动量 |
| threshold | 阈值 |

每个点代表一天的计算结果：

- **日收益率**：r_t = ln(P_t / P_{t-1})
- **ReLU 收益**：relu_t = max(0, r_t - threshold)
- **累计动量**：M_t = Σ relu_t

曲线不能只按股票保存，因为同一股票使用不同阈值和时间窗口，结果可能不同。**最好增加：**

- `analysis_id` — 分析批次 ID
- `point_index` — 点序号

用于区分不同分析批次。

---

### 3.7 stock_prices

保存原始行情。

| 字段 | 说明 |
|------|------|
| symbol | 股票代码 |
| trade_date | 交易日 |
| close_price | 收盘价 |
| volume | 成交量 |
| return_pct | 收益率 |

建议建立唯一约束：

```sql
unique(symbol, trade_date)
```

同一股票同一个交易日只能有一条行情记录。

---

## 4. 建议增加分析快照表

原始设计中可以增加一张 `analysis_runs`，用于保存一次完整分析。

| 字段 | 说明 |
|------|------|
| id | 主键 |
| news_id | 关联新闻 ID |
| status | 状态 |
| model_version | 模型版本 |
| parameters | 参数 |
| started_at | 开始时间 |
| completed_at | 完成时间 |
| error_message | 错误信息 |

**原因**：同一篇新闻以后可能使用不同模型重新分析。如果直接覆盖原结果，就无法知道：

- 当时使用了哪个模型版本
- 当时的 threshold 是多少
- 当时使用了什么权重
- 为什么以前的结果和现在不同

**推荐关系：**

```
news_articles
    ↓
analysis_runs
    ↓
  events
    ↓
stock_impacts
    ↓
factor_scores
relu_momentum_points
```

---

## 5. 持久化流程

一次分析完成后，应该在一个事务中保存：

1. 保存或查找新闻
2. 创建 analysis_run
3. 保存事件
4. 保存关联公司
5. 保存股票影响结果
6. 保存全部因子
7. 保存 ReLU 曲线点
8. 更新分析状态为成功

Spring Boot 中建议由独立的 `AnalysisPersistenceService` 负责：

```java
@Transactional
public void saveAnalysis(AnalysisContext context) {
    // 保存新闻
    // 保存分析运行记录
    // 保存事件
    // 保存关联公司
    // 保存股票影响
    // 保存因子和曲线
}
```

这样任意一步失败时，整个事务回滚，避免出现：

- 新闻保存了，但事件没有保存
- 事件保存了，但股票评分没有保存
- 评分保存了，但曲线数据缺失

---

## 6. JPA 和 Flyway

当前项目代码还没有真正接入 PostgreSQL。接入时需要增加依赖：

```xml
spring-boot-starter-data-jpa
postgresql
flyway-core
```

配置示例：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/factorx
spring.datasource.username=factorx
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

**不要在正式环境使用：**

```properties
spring.jpa.hibernate.ddl-auto=create
spring.jpa.hibernate.ddl-auto=create-drop
```

因为这可能删除已有数据。

数据库表结构应该通过 Flyway 管理：

```
V1__init.sql
V2__add_analysis_runs.sql
V3__add_backtest_labels.sql
```

---

## 7. 常用索引

```sql
create unique index uk_news_hash
on news_articles(hash);

create index idx_news_published_at
on news_articles(published_at desc);

create index idx_event_type
on events(event_type);

create index idx_stock_impacts_symbol
on stock_impacts(symbol);

create index idx_stock_prices_symbol_date
on stock_prices(symbol, trade_date desc);

create index idx_relu_points_analysis
on relu_momentum_points(analysis_id, point_index);
```

最重要的查询是：

```sql
select *
from stock_impacts
where symbol = 'TSLA'
order by id desc;
```

用于查看某只股票的历史新闻影响评估。

---

## 8. 后续回测

PostgreSQL 保存的是"当时模型的预测"，不能直接覆盖成实际结果。

后续可以根据新闻发布时间，计算：

- T+1 实际收益
- T+3 实际收益
- T+5 实际收益
- T+10 实际收益

然后与模型预测对比：

- 预测方向是否正确
- 预测概率是否可靠
- 预测涨跌幅是否接近
- 哪些因子最有效

这就是 PostgreSQL 存储对 FactorX 最重要的价值：**保存完整历史，让模型能够被验证、比较和训练。**
