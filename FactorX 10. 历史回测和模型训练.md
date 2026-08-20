# FactorX 10. 历史回测和模型训练

## 概述

这一部分的核心目标，不是简单计算"新闻发布后股票涨了还是跌了"，而是验证 FactorX 的因子、评分和预测区间是否具有稳定的实际效果，并将有效的规则逐步转化为机器学习模型。

文档中已经定义了以下任务：

- 构建历史事件回测框架
- 生成未来 1/3/5/10 个交易日的标签
- 训练机器学习模型
- 输出分类、收益和风险指标
- 根据历史结果优化因子权重

下面对完整实现方式进行展开。

---

## 10.1 回测系统解决什么问题

FactorX 当前的评分逻辑主要基于：

- 新闻事件方向
- 项目规模
- 新闻来源可信度
- 公司相关性
- 行业热度
- 市场确认因子
- ReLU 非对称动量
- 股票与事件的相关程度

例如：

```
一条新闻
  → 提取事件
  → 匹配公司和股票
  → 计算事件因子
  → 计算 ReLU 动量因子
  → 生成影响评分
  → 预测未来走势
```

回测就是把这套流程应用到过去已经发生的新闻上：

```
历史新闻
  → 只使用当时能够获得的信息
  → 计算当时的因子和评分
  → 观察新闻发布后的真实价格表现
  → 对比预测和结果
```

回测需要回答几个问题：

- 高评分股票是否真的更容易上涨？
- 利空事件是否真的具有负向预测能力？
- ReLU 动量因子是否比单纯新闻方向更有效？
- 预测的 1 日、3 日、5 日和 10 日周期哪个最可靠？
- 哪些行业效果较好，哪些行业容易失效？
- 评分是否只是看起来合理，但没有真实收益能力？
- 模型在不同市场环境下是否稳定？

---

## 10.2 历史回测的基本数据结构

一次回测样本通常对应：

> 一条新闻事件 + 一只相关股票 + 一个事件发生时间

例如：

- 新闻：某公司获得大型储能项目订单
- 事件时间：2024-03-15 10:30
- 股票：TSLA
- 事件方向：利好

对应的回测样本可能是：

| 字段 | 示例 |
|------|------|
| event_id | 10086 |
| symbol | TSLA |
| event_time | 2024-03-15 10:30 |
| event_direction | positive |
| event_score | 0.78 |
| stock_relevance_score | 0.82 |
| relu_momentum_score | 0.61 |
| final_impact_score | 0.75 |
| prediction_direction | up |
| prediction_probability | 0.75 |
| price_at_event | 172.30 |
| price_1d_after | 175.10 |
| price_3d_after | 178.60 |
| price_5d_after | 181.20 |
| price_10d_after | 176.90 |

回测样本必须保留"当时的输入"和"之后的真实结果"，不能只保存最终统计值。否则无法重新训练模型，也无法定位某个因子是否产生了错误判断。

---

## 10.3 最重要的问题：防止未来数据泄漏

历史回测最容易出现的问题是"未来数据泄漏"。

所谓未来数据泄漏，是指模型在计算某个历史时刻的预测时，使用了当时还没有发生的信息。

例如：

> 新闻发生时间：2024-03-15 10:30

**此时可以使用：**

- 新闻发布时间
- 新闻正文
- 之前的历史行情
- 之前已经公布的公司和行业数据
- 新闻发生前的成交量和价格走势

**不能使用：**

- 2024-03-15 收盘价，如果新闻发生在盘中
- 2024-03-16 之后的行情计算 ReLU 因子
- 后续新闻对原新闻的补充内容
- 未来修订后的公司行业分类
- 包含未来时间段数据计算出的标准化参数
- 使用全量数据训练后再预测历史样本

**正确原则是：**

> 预测时点 T 的特征，只能由 T 及 T 之前的数据生成；标签，才可以使用 T 之后的数据。

因此事件时间必须精确到时间戳，而不能只保存日期。

---

## 10.4 事件时间和交易时间处理

新闻可能发生在不同时间：

### 盘前发布

新闻时间：09:00

通常可以使用当天开盘价作为事件后的第一个可交易价格。

### 盘中发布

新闻时间：10:30

不能直接使用当天收盘价作为事件价格。更合理的处理是：

> 事件价格 = 新闻发布后第一个可执行价格

可以采用：

- 新闻发布后 5 分钟收盘价
- 新闻发布后 15 分钟收盘价
- 新闻发布后第一个完整 K 线的收盘价
- 如果只有日线，则使用下一交易日开盘价

### 盘后发布

新闻时间：18:00

当天已经不能交易，事件后的第一个交易价格通常是下一交易日开盘价。

系统需要统一定义事件价格规则，例如：

```
effective_entry_time = 新闻发布后第一个可交易时间
```

否则不同新闻的回测结果无法进行比较。

---

## 10.5 训练标签设计

文档定义了四类标签：

| 标签 | 含义 |
|------|------|
| label_1d_up | 1 个交易日后是否上涨 |
| label_3d_up | 3 个交易日后是否上涨 |
| label_5d_return | 5 个交易日收益率 |
| label_10d_return | 10 个交易日收益率 |

具体计算方式可以定义为：

```
return_1d  = price_T+1 / price_T - 1
return_3d  = price_T+3 / price_T - 1
return_5d  = price_T+5 / price_T - 1
return_10d = price_T+10 / price_T - 1
```

涨跌标签：

```
label_1d_up = 1, 如果 return_1d > 0
label_1d_up = 0, 否则
```

但是实际系统不建议只使用"是否上涨"作为唯一标签，因为股票在某一天上涨 0.01% 和上涨 8% 的意义完全不同。

可以同时保存以下标签：

- `return_1d`
- `return_3d`
- `return_5d`
- `return_10d`
- `excess_return_1d`
- `excess_return_3d`
- `excess_return_5d`
- `excess_return_10d`

其中超额收益表示股票收益减去基准收益：

```
excess_return_5d = stock_return_5d - benchmark_return_5d
```

基准可以选择：

- 标普 500
- 纳斯达克指数
- 沪深 300
- 对应市场指数
- 对应行业指数

这样可以区分：

- 股票因为整个市场上涨而上涨
- 股票因为这条新闻而相对跑赢市场

---

## 10.6 新闻事件样本的标签方向

对于利好和利空事件，最好不要只统一判断"股票是否上涨"，还要考虑事件方向。

例如：

- 利好事件后上涨：预测正确
- 利好事件后下跌：预测错误
- 利空事件后下跌：预测正确
- 利空事件后上涨：预测错误

可以定义方向收益：

```
signed_return =
  return，如果事件方向为利好
  -return，如果事件方向为利空
```

这样：

```
signed_return > 0
```

就表示事件方向和实际价格反应一致。

对应的方向标签：

```
label_direction_correct =
  1, 如果 signed_return > 0
  0, 否则
```

这比单纯判断股票涨跌更加符合 FactorX 的业务目标。

---

## 10.7 特征工程

机器学习模型的输入不是新闻原文，而是结构化后的特征。

### 事件特征

包括：

- `event_type`
- `event_direction`
- `project_amount`
- `project_scale_score`
- `source_credibility`
- `company_relevance`
- `sector_heat`
- `country`
- `event_strength`

事件类型可以编码为：

- 合同订单
- 投资
- 并购
- 政策补贴
- 制裁
- 诉讼
- 延迟
- 产品召回
- 监管调查

### 股票相关性特征

包括：

- `stock_relevance_score`
- `company_match_score`
- `sector_match_score`
- `business_match_score`
- `revenue_exposure`

例如某公司新闻涉及芯片项目，那么芯片业务占比高的公司，相关性应该高于业务关联较弱的公司。

### ReLU 动量特征

根据已有设计，可以使用：

- `relu_slope`
- `positive_density`
- `plateau_ratio`
- `momentum_purity`
- `relu_momentum`

还可以增加：

- `return_5d_before_event`
- `return_20d_before_event`
- `volatility_20d`
- `volume_ratio`
- `distance_to_ma20`
- `distance_to_ma60`

这些特征必须全部使用事件发生前的数据。

### 市场状态特征

模型还应知道当前处于什么市场环境：

- `market_return_1d`
- `market_return_5d`
- `market_volatility`
- `sector_return_5d`
- `sector_volume_ratio`
- `index_above_ma20`
- `index_above_ma60`

同一条利好新闻，在牛市和熊市中的价格反应可能完全不同。

---

## 10.8 规则模型和机器学习模型的关系

FactorX 不需要一开始就完全放弃规则模型。

更合理的升级路径是：

```
规则模型
  → 规则模型回测
  → 规则权重优化
  → 机器学习基线
  → 树模型
  → 非线性模型
  → 线上模型服务
```

### 第一阶段：规则模型

使用当前公式：

```
finalImpactScore =
    0.45 * eventScore
  + 0.30 * stockRelevanceScore
  + 0.25 * reluMomentumScore
```

回测这个公式，确认基础规则是否有预测能力。

### 第二阶段：规则权重优化

将固定权重 `0.45 / 0.30 / 0.25` 改为可调参数：

- `w_event`
- `w_relevance`
- `w_relu`

约束：

```
w_event + w_relevance + w_relu = 1
```

然后在训练集上寻找表现更好的权重组合。

需要注意，不能直接在测试集上调权重，否则测试集就失去了独立验证作用。

### 第三阶段：Logistic Regression

Logistic Regression 适合作为第一个机器学习基线模型。

**输入：**

- `event_score`
- `stock_relevance_score`
- `relu_momentum_score`
- `source_credibility`
- `sector_heat`
- `volume_ratio`
- `market_return_5d`

**输出：** 未来上涨概率

**优点：**

- 解释性强
- 训练速度快
- 容易发现特征方向
- 适合验证特征是否有基本价值

### 第四阶段：XGBoost 或 LightGBM

树模型适合处理：

- 非线性关系
- 特征之间的交互
- 缺失值
- 不同量纲的特征
- 事件类型和市场环境的组合影响

例如：

- 利好事件 + 高来源可信度 + 高 ReLU 动量 → 可能产生强正向效果
- 利好事件 + 高 ReLU 动量 + 价格已经短期大涨 → 可能反而代表利好已经被提前交易

XGBoost 或 LightGBM 可以输出：

- 上涨概率
- 特征重要性
- SHAP 解释
- 不同特征组合的贡献

### 第五阶段：PyTorch MLP + ReLU

PyTorch MLP 可以用于复杂的非线性模型，但不建议作为第一版模型。

**适合使用的场景：**

- 样本量足够大
- 特征维度较高
- 已经有稳定的数据清洗流程
- 树模型已经验证过特征有效
- 需要建模更复杂的交互关系

如果样本量较小，MLP 很容易过拟合，表面上的回测成绩可能很好，但上线后表现明显下降。

---

## 10.9 时间序列数据如何划分

金融数据不能随机打乱后再划分训练集和测试集。

**错误方式：** 随机抽取 80% 训练，20% 测试

原因是未来样本可能被分到训练集，过去样本被分到测试集，模型实际上提前看到了未来市场环境。

### 推荐按时间划分

```
训练集：2021-01-01 至 2023-12-31
验证集：2024-01-01 至 2024-06-30
测试集：2024-07-01 至 2024-12-31
```

### 滚动回测

```
训练 2021 年 → 验证 2022 年 1 月
训练 2021 年至 2022 年 1 月 → 验证 2022 年 2 月
训练扩展到 2022 年 2 月 → 验证 2022 年 3 月
```

更接近真实线上场景的是 **Walk-forward 验证**：

```
历史数据训练
  → 预测下一段时间
  → 加入新数据重新训练
  → 再预测下一段时间
```

这样能够观察模型是否随着市场变化而失效。

---

## 10.10 回测流程的完整步骤

一次完整的历史回测可以分为以下阶段。

### 第一步：确定回测范围

输入：

```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "symbols": ["TSLA", "NVDA"]
}
```

还可以增加：

```json
{
  "horizon": [1, 3, 5, 10],
  "eventTypes": ["contract", "investment"],
  "minScore": 0.5,
  "benchmark": "NASDAQ",
  "transactionCost": 0.001
}
```

### 第二步：读取历史新闻

根据发布时间查询新闻：

```
published_at >= startDate
published_at <= endDate
```

需要进行：

- 新闻去重
- 时间统一
- 来源过滤
- 新闻正文完整性检查
- 事件发布时间校验

### 第三步：重建当时的事件因子

不能直接读取今天重新分析出的结果，因为规则、词典和模型可能已经变化。

需要使用当时版本的解析逻辑，重新生成：

- `event_type`
- `event_direction`
- `project_scale_score`
- `source_credibility`
- `company_relevance`
- `sector_heat`

最好记录版本号：

- `extractor_version`
- `factor_version`
- `model_version`

### 第四步：匹配股票

将事件映射到股票：

```
event → company → symbol
```

同时记录匹配方式：

- `direct_company_match`
- `sector_match`
- `keyword_match`
- `manual_mapping`

后续可以分别评估不同匹配策略的准确性。

### 第五步：计算事件前因子

只读取事件发生前的行情，计算：

- `relu_momentum`
- `positive_density`
- `plateau_ratio`
- `momentum_purity`
- `volume_ratio`
- `volatility`
- `market_state`

### 第六步：生成预测

使用规则模型或机器学习模型输出：

- `prediction_direction`
- `prediction_probability`
- `predicted_return`
- `confidence`

### 第七步：生成未来标签

从事件价格开始，查询未来 1、3、5、10 个交易日的价格，生成真实收益。

### 第八步：计算单笔交易结果

对于每个事件和股票，计算：

- `actual_return`
- `direction_correct`
- `profit_after_cost`
- `holding_period`
- `maximum_adverse_excursion`
- `maximum_favorable_excursion`

### 第九步：汇总结果

按以下维度聚合：

- 整体
- 时间
- 行业
- 事件类型
- 利好/利空
- 股票
- 模型版本
- 分数区间
- 市场状态

---

## 10.11 评估指标说明

文档中定义的指标可以分为三组。

### 一、分类指标

#### 方向准确率

```
accuracy = 预测正确的样本数 / 总样本数
```

例如：1000 条事件中，620 条方向判断正确 → `accuracy = 0.62`

但当上涨和下跌样本不均衡时，单独看准确率不够。

#### AUC

AUC 衡量模型对正样本和负样本的排序能力。

- `AUC = 0.5`：接近随机
- `AUC > 0.6`：通常说明存在一定排序能力
- `AUC > 0.7`：说明模型具有较明显的区分能力，但仍需结合收益和交易成本判断

### 二、收益指标

#### 平均收益

```
avgReturn = 所有交易收益率的平均值
```

应该同时区分：

- `grossReturn`
- `netReturn`

净收益需要扣除：

- 手续费
- 滑点
- 汇率成本
- 交易税
- 借券成本
- 买卖价差

#### 胜率

```
winRate = 盈利交易数 / 总交易数
```

胜率高不一定赚钱。例如：胜率 80%，但每次盈利 0.2%，亏损一次 5%，整体仍然可能亏损。

#### 盈亏比

```
profitLossRatio = 平均盈利金额 / 平均亏损金额
```

当胜率为 58%、盈亏比为 1.4 时，理论期望值为：

```
期望收益 = 胜率 × 平均盈利 - 失败率 × 平均亏损
```

还需要扣除交易成本后重新计算。

#### 最大回撤

最大回撤表示净值从历史高点跌到后续低点的最大幅度。

```
maxDrawdown = 当前净值 / 历史最高净值 - 1
```

例如：净值从 1.20 降到 1.10 → 最大回撤 = -8.33%

最大回撤通常比平均收益更能反映策略风险。

### 三、稳定性指标

应进一步计算：

- 按月份收益
- 按季度收益
- 不同行业收益
- 不同市况收益
- 不同分数区间收益
- 利好和利空分别表现
- 样本数量
- 置信区间
- 预测概率校准度

尤其要避免只报告整体平均值，因为整体结果可能被少数极端行情主导。

---

## 10.12 预测概率必须校准

FactorX 当前概率转换逻辑类似：

```
probability = 45 + finalImpactScore * 40
```

这更像是评分到展示概率的线性映射，不一定是真实概率。

如果系统显示：

> 预测上涨概率：75%

那么长期来看，所有被预测为 75% 的样本，实际上涨比例应该接近 75%。

可以使用以下方式校准：

- Platt Scaling
- Isotonic Regression
- Reliability Curve
- Brier Score

例如按预测概率分组：

| 预测概率区间 | 实际上涨比例 |
|-------------|-------------|
| 50%～55% | 52% |
| 55%～65% | 58% |
| 65%～75% | 64% |
| 75%～85% | 68% |

如果预测 75% 的样本实际只有 68% 上涨，说明系统过度自信，需要校准。

因此产品中的"概率"应明确区分：

- `score-based probability`
- `calibrated statistical probability`

---

## 10.13 因子权重优化

当前规则权重为：

| 因子 | 权重 |
|------|------|
| eventScore | 0.45 |
| stockRelevanceScore | 0.30 |
| reluMomentumScore | 0.25 |

可以使用历史训练集来优化：

```
w1 * eventScore + w2 * stockRelevanceScore + w3 * reluMomentumScore
```

约束条件：

```
w1 >= 0
w2 >= 0
w3 >= 0
w1 + w2 + w3 = 1
```

优化目标可以选择：

- 最大化验证集上的净平均收益
- 最大化方向准确率

但更建议使用综合目标，例如：

```
objective =
  0.4 * AUC
+ 0.3 * netReturn
+ 0.2 * winRate
- 0.1 * drawdownPenalty
```

权重优化时必须注意：

- 只使用训练集和验证集
- 测试集只用于最后一次评估
- 保存每次权重优化结果
- 防止频繁调参导致过拟合
- 同时观察行业和时间分组表现

如果优化后整体收益提高，但所有收益都来自某一个行业或某一段行情，说明权重不一定具有普适性。

---

## 10.14 行业分组评估

文档要求输出行业表现，例如：

```json
{
  "bySector": {
    "新能源": {
      "accuracy": 0.65
    },
    "半导体": {
      "accuracy": 0.59
    }
  }
}
```

实际输出建议更完整：

```json
{
  "sector": "半导体",
  "sampleCount": 428,
  "directionAccuracy": 0.59,
  "auc": 0.64,
  "avgReturn": 0.018,
  "netReturn": 0.014,
  "winRate": 0.56,
  "maxDrawdown": -0.11,
  "profitLossRatio": 1.28
}
```

行业分组可以发现：

- 某些行业新闻传导速度较快
- 某些行业需要更长持有周期
- 某些行业对成交量确认更敏感
- 某些行业容易出现新闻兑现或反转
- 某些行业的关键词匹配质量较差

如果某行业样本量过少，应标记为 `insufficient_sample`，不能仅凭少量样本得出结论。

---

## 10.15 Spring Boot 与 Python 服务分工

文档给出的架构是：

```
Spring Boot
  → 业务、API、权限、任务调度、数据入库

Python FastAPI
  → 特征处理、训练、回测、模型预测

PostgreSQL
  → 新闻、事件、行情、因子、训练样本、回测结果
```

### Spring Boot 负责

- 创建回测任务
- 校验时间范围和股票池
- 保存任务状态
- 调用 Python 服务
- 查询回测结果
- 向前端返回结果
- 管理模型版本
- 记录操作日志

### Python FastAPI 负责

- 读取训练数据
- 生成特征矩阵
- 训练模型
- 执行历史回测
- 计算评估指标
- 保存模型文件
- 提供单条预测接口

### 推荐的任务状态

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `CANCELLED`

因为回测和训练可能耗时较长，不建议让前端一直等待同步响应。

更合理的接口流程是：

```
POST /api/backtests → 返回 taskId
GET  /api/backtests/{taskId} → 查询任务状态
GET  /api/backtests/{taskId}/result → 查询最终结果
```

---

## 10.16 回测接口建议

文档中的 `POST /backtest` 可以作为 Python 内部接口。请求内容可以扩展为：

```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "symbols": ["TSLA", "NVDA"],
  "horizons": [1, 3, 5, 10],
  "modelVersion": "rule-v1",
  "benchmark": "NASDAQ",
  "transactionCost": 0.001,
  "slippage": 0.0005
}
```

返回结果建议包括：

```json
{
  "taskId": "bt-20240821-001",
  "status": "SUCCESS",
  "sampleCount": 1260,
  "directionAccuracy": 0.62,
  "auc": 0.68,
  "avgReturn": 0.023,
  "netAvgReturn": 0.019,
  "maxDrawdown": -0.08,
  "winRate": 0.58,
  "profitLossRatio": 1.4,
  "bySector": {},
  "byHorizon": {},
  "byScoreBucket": {},
  "modelVersion": "rule-v1"
}
```

---

## 10.17 模型训练接口建议

可以提供以下接口：

### POST /train

请求：

```json
{
  "dataset": "event_samples",
  "trainStart": "2021-01-01",
  "trainEnd": "2023-12-31",
  "validationStart": "2024-01-01",
  "validationEnd": "2024-06-30",
  "target": "label_5d_return",
  "modelType": "xgboost",
  "features": [
    "event_score",
    "stock_relevance_score",
    "relu_momentum_score",
    "volume_ratio",
    "market_return_5d"
  ]
}
```

返回：

```json
{
  "modelVersion": "xgb-5d-v3",
  "target": "label_5d_return",
  "trainSamples": 8500,
  "validationSamples": 2100,
  "auc": 0.71,
  "directionAccuracy": 0.64,
  "featureImportance": {
    "event_score": 0.31,
    "relu_momentum_score": 0.24,
    "stock_relevance_score": 0.18
  }
}
```

### POST /predict

请求：

```json
{
  "modelVersion": "xgb-5d-v3",
  "features": {
    "event_score": 0.78,
    "stock_relevance_score": 0.82,
    "relu_momentum_score": 0.61,
    "volume_ratio": 1.8,
    "market_return_5d": 0.012
  }
}
```

返回：

```json
{
  "direction": "up",
  "probability": 0.73,
  "expectedReturn": 0.028,
  "modelVersion": "xgb-5d-v3"
}
```

---

## 10.18 模型版本管理

每次训练都应生成独立版本，例如：

- `rule-v1`
- `logistic-5d-v1`
- `xgb-5d-v1`
- `xgb-5d-v2`
- `mlp-10d-v1`

模型记录至少应包括：

- `model_version`
- `model_type`
- `target`
- `feature_schema`
- `training_start`
- `training_end`
- `validation_metrics`
- `test_metrics`
- `hyperparameters`
- `created_at`
- `model_path`
- `status`

线上预测必须记录使用的模型版本，否则之后无法解释：

> 当时为什么给出这个概率？

建议每个预测结果都保存：

- `model_version`
- `factor_version`
- `extractor_version`
- `feature_snapshot`
- `prediction_time`

---

## 10.19 交易成本和现实限制

如果回测不计算交易成本，结果通常会偏乐观。

至少应考虑：

- 手续费
- 滑点
- 买卖价差
- 成交限制
- 停牌
- 涨跌停
- 流动性
- 汇率

对于新闻事件策略，还应考虑：

- 新闻发布后价格可能已经快速跳空
- 预测信号可能无法按理论价格成交
- 高波动股票的滑点会明显增加
- 低成交量股票可能无法按回测数量成交
- 同一天多个新闻可能导致重复交易
- 相同事件可能被多个新闻源重复计算

因此，回测中最好同时输出：

- 理论收益
- 扣除交易成本后的收益
- 不同滑点假设下的收益

---

## 10.20 回测结果的正确解读

不能只看 `accuracy = 0.62`，还需要结合：

- AUC
- 平均净收益
- 最大回撤
- 样本数量
- 交易频率
- 行业稳定性
- 时间稳定性
- 概率校准

例如：

- 方向准确率：62%
- AUC：0.68
- 平均净收益：1.9%
- 最大回撤：-8%
- 胜率：58%
- 盈亏比：1.4

这说明模型可能具备一定排序和交易价值，但还不能直接得出"模型可靠"的结论。

还要检查：

- 结果是否集中在少数几笔交易
- 是否只在某个行业有效
- 是否只在牛市有效
- 是否扣除成本后仍然有效
- 测试集表现是否明显低于训练集
- 模型概率是否过度自信
- 样本量是否足够支持结论

---

## 10.21 推荐落地顺序

任务 10 可以按照以下顺序实现：

### 第一步：规则模型回测

先直接回测现有的：

- `eventScore`
- `stockRelevanceScore`
- `reluMomentumScore`
- `finalImpactScore`

输出各周期的真实收益和方向正确率。

### 第二步：完善历史样本表

为每个事件、股票和时间周期保存：

- 当时的因子
- 预测结果
- 未来收益
- 方向标签
- 行业
- 市场状态

### 第三步：增加训练标签

至少支持：

- 1 日方向
- 3 日方向
- 5 日收益
- 10 日收益

同时增加超额收益标签。

### 第四步：训练 Logistic Regression

用于验证：

- 特征是否有效
- 因子方向是否合理
- 模型是否比规则公式更好

### 第五步：训练 XGBoost 或 LightGBM

用于捕捉：

- 非线性关系
- 因子交互
- 行业差异
- 市场状态差异

### 第六步：进行滚动回测

验证模型在不同时间段的稳定性。

### 第七步：优化规则权重

根据验证结果优化：

- `eventScore`
- `stockRelevanceScore`
- `reluMomentumScore`

### 第八步：部署 FastAPI 预测服务

训练完成后，将模型以版本化方式提供给 Spring Boot 调用。

---

## 10.22 验收标准的具体含义

文档中的验收标准可以具体解释为：

### 能回测历史事件

能够输入：

- 时间范围
- 股票池
- 事件类型
- 预测周期
- 模型版本

并生成回测任务和结果。

### 能输出准确率和 AUC

不仅输出整体指标，还应输出：

- 按预测周期
- 按事件方向
- 按行业
- 按分数区间

### 能输出收益指标

至少包括：

- 平均收益
- 净收益
- 最大回撤
- 胜率
- 盈亏比

### 能按行业评估

每个行业应包含样本数量，否则容易误导。

### 能调整因子权重

权重调整应有：

- 原始权重
- 优化后权重
- 优化目标
- 训练时间范围
- 验证结果

### 训练好的模型能预测

FastAPI 至少要支持：

- 加载指定模型版本
- 接收结构化因子
- 返回预测方向
- 返回概率
- 返回预期收益
- 返回模型版本

---

## 总结

FactorX 的历史回测和模型训练应形成如下闭环：

```
历史新闻
  → 重建事件
  → 计算当时可见的因子
  → 匹配股票
  → 生成预测
  → 获取未来真实收益
  → 计算准确率和风险收益指标
  → 优化因子权重
  → 训练机器学习模型
  → 部署预测服务
```

最关键的工程原则有三个：

1. **严格防止未来数据泄漏**
2. **按时间顺序划分训练集、验证集和测试集**
3. **同时评估预测准确性、实际收益和最大风险**

只有当模型在未参与训练的历史时间段中，扣除交易成本后仍保持相对稳定的表现，FactorX 的评分结果才具有真正的量化参考价值。
