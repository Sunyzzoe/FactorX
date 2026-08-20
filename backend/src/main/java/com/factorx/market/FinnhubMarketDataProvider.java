package com.factorx.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.factorx.market.model.StockPrice;
import com.factorx.market.model.StockQuote;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "factorx.market.finnhub", name = "enabled", havingValue = "true")
public class FinnhubMarketDataProvider implements MarketDataProvider {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutMs;

    public FinnhubMarketDataProvider(
            ObjectMapper objectMapper,
            @Value("${factorx.market.finnhub.base-url:https://finnhub.io/api/v1}") String baseUrl,
            @Value("${factorx.market.finnhub.api-key:}") String apiKey,
            @Value("${factorx.market.http.timeout-ms:10000}") int timeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofMillis(timeoutMs)).build();
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public StockQuote getQuote(String symbol) {
        JsonNode node = get("/quote?symbol=" + encode(symbol));
        return new StockQuote(symbol, decimal(node, "c"), decimal(node, "pc"), decimal(node, "o"),
                decimal(node, "h"), decimal(node, "l"), null,
                Instant.ofEpochSecond(node.path("t").asLong(Instant.now().getEpochSecond())), "USD", name());
    }

    @Override
    public List<StockPrice> getHistory(String symbol, LocalDate startDate, LocalDate endDate) {
        long from = startDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long to = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        JsonNode node = get("/stock/candle?symbol=" + encode(symbol) + "&resolution=D&from=" + from + "&to=" + to);
        if (!"ok".equalsIgnoreCase(node.path("s").asText())) {
            return List.of();
        }
        JsonNode closes = node.path("c");
        JsonNode volumes = node.path("v");
        JsonNode timestamps = node.path("t");
        List<StockPrice> prices = new ArrayList<>();
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal close = closes.get(i).decimalValue();
            long timestamp = timestamps.get(i).asLong();
            Long volume = i < volumes.size() && !volumes.get(i).isNull() ? volumes.get(i).asLong() : null;
            prices.add(new StockPrice(symbol, Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate(),
                    close, close, volume, null, "USD", name()));
        }
        return prices;
    }

    @Override
    public String name() {
        return "finnhub";
    }

    private JsonNode get(String path) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new MarketDataException("Finnhub API key is not configured");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path + (path.contains("?") ? "&" : "?") + "token=" + encode(apiKey)))
                    .timeout(java.time.Duration.ofMillis(timeoutMs)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MarketDataException("Finnhub returned HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MarketDataException("Interrupted while fetching Finnhub market data", ex);
        } catch (IOException ex) {
            throw new MarketDataException("Failed to fetch Finnhub market data", ex);
        }
    }

    private java.math.BigDecimal decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).decimalValue() : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
