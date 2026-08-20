# FactorX 任务 9：接入真实行情数据

## 概述

任务 9 属于第二阶段"数据化"，目标是把此前的模拟价格序列替换成真实市场数据，使 FactorX 的判断同时参考：

- 新闻事件本身
- 股票历史价格
- 成交量变化
- 行业 ETF 表现
- 股票波动率
- 新闻发布后的市场反应

这里的关键不是简单调用一个行情 API，而是建立一条可靠的数据链路：

```
行情数据源
    ↓
行情适配器
    ↓
统一数据模型
    ↓
数据清洗、去重、补算指标
    ↓
PostgreSQL 入库
    ↓
ReLU 动量计算
    ↓
市场确认因子
    ↓
股票影响评分
    ↓
前端展示
```

---

## 一、行情接入的作用

任务 8 接入新闻后，系统只能回答：

> 这条新闻可能影响哪些股票？

任务 9 接入行情后，系统可以进一步回答：

- 市场是否已经对这条新闻产生反应？
- 这只股票当前是否具备正向动量？
- 成交量是否支持价格变化？
- 行业是否同步确认？
- 新闻方向和价格方向是否发生冲突？

因此，行情数据主要承担三个职责：

### 1. 生成 ReLU 动量曲线

使用真实收盘价计算：

```
日收益率
    ↓
ReLU 截断收益
    ↓
ReLU 累计动量
    ↓
动量密度、平台风险、阿尔法纯度
```

### 2. 生成市场确认因子

通过价格、成交量和行业 ETF 判断新闻影响是否已经被市场确认。

### 3. 提供风险提示

例如：

- 新闻利好，但股价下跌
- 股价上涨，但没有成交量支持
- 股票上涨，但行业 ETF 下跌
- 股价波动过大，预测区间需要扩大
- 新闻发布时市场已经提前上涨，可能存在利好兑现

---

## 二、行情数据源选择

文档中列出了 Yahoo Finance、Finnhub、Alpha Vantage 和 Polygon。

### 推荐选择

**MVP 阶段：**

- 主数据源：Finnhub 或 Alpha Vantage
- 备用数据源：Yahoo Finance

**生产阶段：**

如果需要稳定的实时行情、历史数据和商业使用授权，优先考虑：

- Polygon
- Finnhub 商业版
- 交易所授权数据源
- 券商 API

### 各数据源特点

| 数据源 | 优点 | 注意事项 |
|--------|------|----------|
| Yahoo Finance | 使用门槛低、历史数据方便 | 非正式接口，稳定性和授权需要注意 |
| Finnhub | 财经数据完整，适合美股 | API Key、频率限制 |
| Alpha Vantage | 适合 MVP，接口简单 | 免费额度较低 |
| Polygon | 实时和历史数据能力强 | 通常需要付费 |
| 券商 API | 数据和交易连接更直接 | 接入复杂，适合后期 |

### 统一接口定义

建议不要让业务代码直接依赖某一家供应商，而是先定义统一接口：

```java
public interface MarketDataProvider {

    StockQuote getQuote(String symbol);

    List<StockPrice> getHistory(
        String symbol,
        LocalDate startDate,
        LocalDate endDate
    );
}
```

后续可以分别实现：

- `FinnhubMarketDataProvider`
- `AlphaVantageMarketDataProvider`
- `YahooMarketDataProvider`
- `PolygonMarketDataProvider`

业务层只依赖 `MarketDataProvider`，更换数据源时不需要修改分析逻辑。

---

## 三、统一行情数据模型

不同供应商的字段名称和时间格式不一致，需要先转换成 FactorX 内部格式。

### 1. 实时行情模型

```java
public class StockQuote {

    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayOpen;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    private Instant quoteTime;
    private String currency;
    private String provider;
}
```

### 2. 历史行情模型

```java
public class StockPrice {

    private String symbol;
    private LocalDate tradeDate;
    private BigDecimal closePrice;
    private BigDecimal adjustedClosePrice;
    private Long volume;
    private String currency;
    private String provider;
}
```

### 3. 行情原始记录

生产环境建议额外保存原始响应，方便排查数据问题：

```java
public class RawMarketData {

    private String symbol;
    private String provider;
    private String requestDate;
    private String responseBody;
    private Instant fetchedAt;
    private String responseHash;
}
```

原始数据不一定参与业务计算，但出现价格异常时，可以确认是供应商数据问题、转换问题，还是计算问题。

---

## 四、行情数据获取流程

### 1. 首次同步历史数据

每只股票至少需要拉取过去 **60 至 120 个交易日**的数据。

原因是：

- ReLU 计算需要一段历史窗口
- 20 日均量需要至少 20 个交易日
- 波动率需要多个收益率样本
- 新闻发生前需要有基准行情
- 后续回测需要保留足够历史数据

**推荐：**

| 场景 | 历史窗口 |
|------|----------|
| 最小 | 60 个交易日 |
| 推荐 | 120 个交易日 |
| 回测 | 数年历史数据 |

### 2. 增量同步

首次同步完成后，只需要每天或每隔几分钟获取新增数据。

- **日线行情**：美股收盘后同步一次
- **实时或准实时行情**：每 1 至 5 分钟同步一次

### 3. 定时任务

Spring Scheduler 可以承担 MVP 阶段的任务调度：

```java
@Scheduled(cron = "0 30 22 * * MON-FRI")
public void syncDailyMarketData() {
    marketDataSyncService.syncAllTrackedSymbols();
}
```

但是不能简单固定使用北京时间，因为不同市场存在：

- 夏令时
- 冬令时
- 节假日
- 半日交易
- 盘前盘后交易

**更可靠的方式：**

- 使用交易所时区计算交易日
- 根据交易日历判断当天是否开市
- 收盘后再拉取完整日线数据

美股通常使用：`America/New_York`

数据库中的时间建议统一保存为 **UTC**，前端再转换为用户时区。

---

## 五、行情清洗和校验

真实行情不能直接入库使用，必须做基础校验。

### 必须校验的内容

#### 1. 价格合法性

```
closePrice > 0
highPrice >= lowPrice
highPrice >= closePrice
lowPrice <= closePrice
```

#### 2. 成交量合法性

```
volume >= 0
```

#### 3. 日期去重

同一股票同一交易日只能保留一条有效记录：`(symbol, trade_date)`

建议建立唯一约束：

```sql
UNIQUE(symbol, trade_date)
```

#### 4. 数据连续性

交易日不应该强制按自然日连续。周末和节假日没有行情是正常情况。

应该使用**前一个有效交易日**计算收益率，而不是简单取前一天自然日期。

#### 5. 异常跳变

如果某日价格相对前一日突然变化超过合理范围，需要标记，而不是直接删除。

可能原因包括：

- 股票拆分
- 反向拆分
- 分红
- 数据供应商错误
- 极端市场事件

---

## 六、收盘价还是复权收盘价

这是行情接入中非常重要的问题。

### ReLU 和收益率计算建议使用复权价格

收益率公式是：

```
r_t = ln(P_t / P_(t-1))
```

如果使用未经复权的收盘价，股票拆分或现金分红可能被误判为巨大涨跌。

**因此建议：**

- 收益率、波动率、ReLU：使用 `adjusted close`
- 页面展示价格：可以展示 `close price`

内部可以保留两个字段：

- `close_price`
- `adjusted_close_price`

如果数据源没有提供复权价格，应明确标记数据质量状态，避免把未复权数据当作高质量历史数据使用。

---

## 七、日收益率计算

假设某只股票有以下复权收盘价：

- 第 1 天：100
- 第 2 天：103
- 第 3 天：101

使用对数收益率：

```
r2 = ln(103 / 100)
r3 = ln(101 / 103)
```

### Java 实现示例

```java
public BigDecimal calculateLogReturn(
        BigDecimal currentPrice,
        BigDecimal previousPrice
) {
    if (currentPrice == null
            || previousPrice == null
            || currentPrice.signum() <= 0
            || previousPrice.signum() <= 0) {
        return null;
    }

    double current = currentPrice.doubleValue();
    double previous = previousPrice.doubleValue();

    return BigDecimal.valueOf(Math.log(current / previous))
            .setScale(8, RoundingMode.HALF_UP);
}
```

不建议使用 `double` 直接作为最终存储类型。计算时可以使用 `double`，入库时应转成规定精度的 `BigDecimal`。

---

## 八、ReLU 动量计算

任务 5 中定义的公式为：

```
relu_t = max(0, r_t - threshold)
M_t = Σ relu_t
```

例如：`threshold = 0.005`，表示只有单日对数收益率超过约 0.5% 时，才被认为是有效正向动量。

### 计算流程

```java
public ReluResult calculate(
        List<StockPrice> prices,
        BigDecimal threshold
) {
    List<ReluMomentumPoint> points = new ArrayList<>();

    BigDecimal cumulativeMomentum = BigDecimal.ZERO;
    BigDecimal cumulativeReturn = BigDecimal.ZERO;
    int positiveCount = 0;
    int plateauCount = 0;

    for (int i = 1; i < prices.size(); i++) {
        BigDecimal current = prices.get(i).getAdjustedClosePrice();
        BigDecimal previous = prices.get(i - 1).getAdjustedClosePrice();

        BigDecimal returnValue =
                calculateLogReturn(current, previous);

        if (returnValue == null) {
            continue;
        }

        BigDecimal reluReturn =
                returnValue.subtract(threshold).max(BigDecimal.ZERO);

        cumulativeMomentum =
                cumulativeMomentum.add(reluReturn);

        cumulativeReturn =
                cumulativeReturn.add(returnValue);

        if (returnValue.compareTo(threshold) > 0) {
            positiveCount++;
        } else {
            plateauCount++;
        }

        points.add(new ReluMomentumPoint(
                prices.get(i).getTradeDate(),
                returnValue,
                cumulativeReturn,
                reluReturn,
                cumulativeMomentum
        ));
    }

    return buildResult(
            points,
            positiveCount,
            plateauCount,
            threshold
    );
}
```

### 命名注意

文档中的 `returnPct` 命名容易造成误解：

- 如果存储的是 `0.012`，表示 1.2%
- 如果存储的是 `1.2`，表示 1.2 个百分点

建议内部统一使用小数：`0.012 = 1.2%`，前端展示时再乘以 100。

---

## 九、核心 ReLU 指标

### 1. ReLU 斜率

```
reluSlope = M_t / lookbackDays
```

表示平均每天产生多少有效正向动量。

### 2. 正向动量密度

```
positiveDensity = count(r_t > threshold) / lookbackDays
```

表示有效上涨日占比。

### 3. 平台风险

```
plateauRatio = count(r_t <= threshold) / lookbackDays
```

表示没有形成有效正向动量的交易日占比。

### 4. 动量纯度

文档定义为：

```
momentumPurity = reluSlope * positiveDensity * (1 - plateauRatio)
```

实际实现时要注意数值范围。如果 `reluSlope` 没有标准化，最终结果可能超过 1。建议先做截断或归一化：

```
normalizedReluSlope = min(1, reluSlope / slopeReference)
```

然后再计算：

```
momentumPurity = normalizedReluSlope * positiveDensity * (1 - plateauRatio)
```

否则 ReLU 动量因子会对最终评分产生过大影响。

---

## 十、成交量和 20 日均量

市场确认不能只看价格，还要判断成交量是否支持价格变化。

### 成交量比率

```
volumeRatio = currentVolume / averageVolume20
```

其中：

```
averageVolume20 = 过去 20 个有效交易日成交量的平均值
```

### 放量规则

按照任务 9 的规则：

```
volume > 2 × averageVolume20
```

则认为出现明显放量。

### 建议分级

| 成交量比率 | 含义 |
|------------|------|
| < 0.8 | 缩量 |
| 0.8 - 1.5 | 正常 |
| 1.5 - 2.0 | 温和放量 |
| >= 2.0 | 明显放量 |

### 注意事项

不要把"上涨"和"放量"简单等同。以下情况意义不同：

- **放量上涨**：通常是正向确认
- **缩量上涨**：确认力度较弱
- **放量下跌**：风险确认
- **缩量下跌**：可能只是正常回撤

---

## 十一、波动率计算

可以使用过去 20 个交易日的对数收益率计算历史波动率：

```
dailyVolatility = std(logReturns20)
annualizedVolatility = dailyVolatility × sqrt(252)
```

Java 计算时可以使用统计库，也可以实现基础标准差计算。

### 波动率的用途

- 调整预估涨跌幅区间
- 识别异常风险
- 判断收益是否只是正常波动
- 避免把高波动股票的普通上涨误判为重大事件反应

**例如：**

新闻利好 + 上涨 3%

- 低波动股票：可能是强确认
- 高波动股票：可能只是普通噪声

---

## 十二、行业 ETF 确认

任务 9 要求加入行业 ETF 表现，用来判断单只股票的上涨是否得到行业支持。

### 示例映射

| 行业 | ETF |
|------|-----|
| 新能源 | ICLN、TAN |
| 半导体 | SOXX、SMH |
| AI / 科技 | QQQ |
| 金融 | XLF |
| 能源 | XLE |
| 生物医药 | XBI |

### 配置表设计

行业 ETF 不应该硬编码在评分逻辑中，建议建立配置表：

```
sector_etf_mapping
-------------------
sector
etf_symbol
priority
active
```

### 判断逻辑

```
股票上涨 + 行业 ETF 上涨
    → 行业确认增强

股票上涨 + 行业 ETF 下跌
    → 个股独立行情，确认减弱

股票下跌 + 行业 ETF 也下跌
    → 系统性风险增强

股票下跌 + 行业 ETF 上涨
    → 个股特异性风险增强
```

---

## 十三、市场确认因子设计

可以将市场确认设计成 0 到 1 的标准化分数。

```
marketConfirmationScore =
    0.40 * volumeConfirmation
  + 0.30 * priceDirectionConfirmation
  + 0.20 * sectorEtfConfirmation
  + 0.10 * volatilityAdjustment
```

### 1. 成交量确认

```
volumeConfirmation = min(volumeRatio / 2, 1)
```

### 2. 价格方向确认

如果新闻方向为利好：

```
stockReturn > 0 → 1
stockReturn = 0 → 0.5
stockReturn < 0 → 0
```

如果新闻方向为利空，则方向相反。

### 3. 行业 ETF 确认

```
股票和 ETF 同方向 → 1
ETF 无明显变化 → 0.5
股票和 ETF 反方向 → 0
```

### 4. 反向运动风险

除了降低确认分数，还应单独生成风险提示：

```
新闻方向：利好
股票 1 日收益：-3.2%
行业 ETF：+0.4%
```

可以输出：

> 风险提示：新闻方向与股票短期价格反向，市场尚未确认该事件，或存在利好兑现、事件不及预期、个股特异性风险。

---

## 十四、新闻发布时间与行情时间对齐

这是最容易造成错误判断的地方。

不能用新闻发布后的收盘价直接判断新闻发布瞬间的市场反应，必须根据新闻发布时间区分：

### 1. 盘前新闻

例如新闻在美股开盘前发布：

- 基准价格：前一交易日收盘价
- 反应价格：当日收盘价或开盘后指定时间价格

### 2. 盘中新闻

例如新闻在交易时间内发布：

- 基准价格：新闻发布前最近一个有效价格
- 反应价格：发布后 30 分钟、收盘或次日收盘

### 3. 盘后新闻

- 基准价格：当日收盘价
- 反应价格：下一个交易日开盘或收盘价

### MVP 简化方案

如果 MVP 只支持日线数据，建议明确采用：

- 新闻发布前最近一个交易日收盘价
- 新闻发布后第 1、3、5、10 个交易日收益

这样可以避免使用未来数据，也方便任务 10 做历史回测。

---

## 十五、数据库入库策略

文档已有 `stock_prices` 表，但真实行情场景还需要处理数据源、复权价格、波动率和更新时间。

可以在实现层补充这些字段或单独建立行情指标表：

```
stock_prices
-------------
symbol
trade_date
close_price
adjusted_close_price
volume
return_pct
volatility_20d
volume_avg_20d
volume_ratio
provider
fetched_at
```

### 核心唯一键

如果保留多供应商数据：

```
(symbol, trade_date, provider)
```

如果只保留统一后的最终数据：

```
(symbol, trade_date)
```

### Upsert 策略

同步时采用 Upsert：

- 不存在 → INSERT
- 已存在且数据更新 → UPDATE
- 已存在且内容相同 → 跳过

不能每次定时任务都重复插入同一天数据，否则会导致：

- ReLU 曲线重复计算
- 成交量统计错误
- 影响记录数量膨胀
- 回测数据重复

---

## 十六、行情 Service 的推荐结构

```
market/
├── controller/
│   └── MarketDataController
├── service/
│   ├── MarketDataService
│   ├── MarketDataSyncService
│   ├── MarketIndicatorService
│   ├── MarketConfirmationService
│   └── TradingCalendarService
├── provider/
│   ├── MarketDataProvider
│   ├── FinnhubMarketDataProvider
│   └── YahooMarketDataProvider
├── model/
│   ├── StockQuote
│   ├── StockPrice
│   ├── MarketIndicators
│   └── MarketConfirmation
└── repository/
    └── StockPriceRepository
```

### Service 接口

```java
public interface MarketDataService {

    StockQuote getQuote(String symbol);

    List<StockPrice> getHistory(
            String symbol,
            int tradingDays
    );

    MarketIndicators calculateIndicators(
            String symbol,
            int tradingDays
    );

    MarketConfirmation checkConfirmation(
            String symbol,
            String direction,
            Instant eventTime
    );
}
```

---

## 十七、与分析流程的结合

原来的分析流程是：

```
新闻
  → 事件抽取
  → 股票匹配
  → ReLU 计算
  → 评分
```

接入真实行情后变为：

```
新闻
  → 事件抽取
  → 股票匹配
  → 获取股票历史行情
  → 获取行业 ETF 行情
  → 计算收益率、成交量、波动率
  → 计算 ReLU 动量
  → 计算市场确认
  → 股票影响评分
  → 输出解释和风险提示
```

对每一只匹配股票，都需要独立计算：

- `stockPrices`
- `reluResult`
- `marketIndicators`
- `marketConfirmation`
- `stockImpact`

### 最终评分公式

```
finalImpactScore =
    0.30 * eventScore
  + 0.25 * stockRelevanceScore
  + 0.20 * reluMomentumScore
  + 0.15 * sourceCredibilityScore
  + 0.10 * marketConfirmationScore
```

这里的 `marketConfirmationScore` 必须使用事件发生时点之前或之后允许使用的数据，不能读取未来行情。

---

## 十八、API 返回结果建议

原有 `/api/analyze` 可以在 `stocks` 中增加市场数据字段：

```json
{
  "symbol": "TSLA",
  "company": "Tesla",
  "direction": "利好",
  "probability": 74,
  "estimatedMove": "+2.0% ~ +5.5%",
  "horizon": "3-10个交易日",
  "marketData": {
    "lastPrice": 245.31,
    "latestTradeDate": "2026-08-20",
    "return1d": 0.018,
    "return5d": 0.042,
    "volume": 82300000,
    "averageVolume20d": 51200000,
    "volumeRatio": 1.61,
    "volatility20d": 0.284,
    "industryEtf": "ICLN",
    "industryEtfReturn1d": 0.009
  },
  "marketConfirmation": {
    "score": 0.78,
    "priceConfirmed": true,
    "volumeConfirmed": false,
    "sectorConfirmed": true,
    "conflict": false,
    "riskNote": null
  }
}
```

这样前端可以展示：

- 最新价格
- 1 日和 5 日收益
- 放量情况
- 行业 ETF 状态
- 市场确认强度
- 新闻与价格是否冲突

---

## 十九、行情不可用时的处理

真实行情接口可能出现：

- API 超时
- 达到调用次数上限
- 股票代码不存在
- 交易所休市
- 数据尚未更新
- 某些股票没有足够历史数据

不能因为行情不可用就让整个新闻分析接口失败。

### 降级策略

| 情况 | 处理 |
|------|------|
| 有完整行情 | 正常计算全部因子 |
| 有部分行情 | 计算可用指标，标记数据不完整 |
| 没有行情 | 不生成伪造数据，返回行情缺失状态 |

**示例：**

```json
{
  "marketDataStatus": "UNAVAILABLE",
  "marketConfirmation": null,
  "riskNote": "当前无法获取最新行情，评分未包含市场确认因子"
}
```

不建议继续使用模拟行情伪装成真实行情，否则前端会误以为市场确认有效。

---

## 二十、任务 9 的实际开发顺序

建议按照以下顺序实现：

### 第一步：定义统一行情接口

- `MarketDataProvider`
- `StockQuote`
- `StockPrice`

### 第二步：接入一个数据源

先只支持一个供应商，完成：

- 单只股票查询
- 历史行情查询
- API Key 配置
- 超时和错误处理

### 第三步：完成数据清洗和入库

实现：

- 复权价格处理
- 日期转换
- Upsert
- 唯一约束
- 原始数据日志

### 第四步：替换 ReLU 模拟数据

让 `ReluFactorService` 接收 `List<StockPrice>`，而不是模拟价格序列。

### 第五步：实现成交量和波动率指标

包括：

- 20 日均量
- `volumeRatio`
- 20 日波动率
- 1 日、5 日收益率

### 第六步：实现市场确认规则

加入：

- 价格方向
- 成交量
- 行业 ETF
- 反向运动风险

### 第七步：接入定时同步

实现：

- 历史数据初始化
- 每日收盘同步
- 失败重试
- 接口限流

### 第八步：接入 /api/analyze

将真实行情计算结果传入：

- `ReluFactorService`
- `ScoringService`
- `ExplanationService`

### 第九步：前端替换模拟状态

展示真实的：

- ReLU 曲线
- 当前价格
- 成交量比率
- 行业 ETF
- 市场确认
- 数据更新时间

---

## 二十一、验收测试

任务 9 至少需要覆盖以下测试。

### 数据层测试

- 能成功获取指定股票历史行情
- 能处理空数据和异常响应
- 同一股票同一交易日不会重复入库
- 行情时间统一转换正确
- 拆分或分红数据不会产生异常收益

### 计算层测试

- 日收益率计算正确
- ReLU 截断逻辑正确
- 20 日均量计算正确
- 成交量超过 2 倍时能识别为放量
- 波动率计算正确
- 行业 ETF 同向时确认增强
- 股票反向运动时生成风险提示

### 集成测试

- `/api/analyze` 使用真实行情生成 ReLU 曲线
- 行情不可用时接口仍能返回分析结果
- 定时任务能完成行情同步
- 前端能显示行情更新时间和数据状态
- 新闻发布时间不会导致未来数据泄漏

### 最终验收标准

```
输入一条新闻
  → 匹配 TSLA
  → 获取 TSLA 真实历史价格
  → 计算真实收益率
  → 生成真实 ReLU 曲线
  → 计算成交量和波动率
  → 获取行业 ETF 表现
  → 判断市场确认
  → 输出影响评分、涨跌幅区间和风险提示
```

任务 9 完成的核心标志不是"API 能返回一个价格"，而是：

> FactorX 的 ReLU 曲线、市场确认因子和最终影响评分，已经完全基于可追溯的真实行情数据计算，并且能够正确处理交易时间、复权价格、缺失数据和重复同步。
