package com.factorx.news;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "factorx.news.finnhub", name = "enabled", havingValue = "true")
public class FinnhubNewsSourceAdapter implements NewsSourceAdapter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String category;

    public FinnhubNewsSourceAdapter(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${factorx.news.finnhub.base-url:https://finnhub.io/api/v1}") String baseUrl,
            @Value("${FACTORX_FINNHUB_API_KEY:}") String apiKey,
            @Value("${factorx.news.finnhub.category:general}") String category,
            @Value("${factorx.news.http.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${factorx.news.http.read-timeout-ms:10000}") int readTimeoutMs
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.category = category;
    }

    @Override
    public String sourceCode() {
        return "finnhub";
    }

    @Override
    public List<RawNewsItem> fetch(Instant since) {
        if (apiKey.isBlank()) {
            throw new NewsSourceException("FACTORX_FINNHUB_API_KEY 未配置", false);
        }
        String payload;
        try {
            payload = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/news")
                            .queryParam("category", category)
                            .queryParam("token", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new NewsSourceException("Finnhub 请求失败", ex, isRetryable(ex));
        }

        try {
            JsonNode root = objectMapper.readTree(payload == null ? "[]" : payload);
            List<RawNewsItem> result = new ArrayList<>();
            for (JsonNode node : root) {
                Instant publishedAt = node.hasNonNull("datetime")
                        ? Instant.ofEpochSecond(node.get("datetime").asLong())
                        : null;
                String title = node.path("headline").asText("");
                String url = node.path("url").asText("");
                if (title.isBlank() || url.isBlank() || publishedAt == null) {
                    continue;
                }
                if (since != null && !publishedAt.isAfter(since)) {
                    continue;
                }
                result.add(new RawNewsItem(
                        node.path("id").asText(""),
                        title,
                        node.path("summary").asText(""),
                        node.path("source").asText("Finnhub"),
                        sourceCode(),
                        url,
                        publishedAt,
                        "en",
                        null,
                        node.path("category").asText("")
                ));
            }
            return result;
        } catch (Exception ex) {
            throw new NewsSourceException("Finnhub JSON 解析失败", ex, false);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof RestClientResponseException response) {
            return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
