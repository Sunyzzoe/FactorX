package com.factorx.news;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "factorx.news.rss", name = "enabled", havingValue = "true")
public class RssNewsSourceAdapter implements NewsSourceAdapter {

    private final RestClient restClient;
    private final NewsTextNormalizer normalizer;
    private final String feedUrl;
    private final String sourceName;

    public RssNewsSourceAdapter(
            RestClient.Builder restClientBuilder,
            NewsTextNormalizer normalizer,
            @Value("${factorx.news.rss.url:}") String feedUrl,
            @Value("${factorx.news.rss.source:RSS}") String sourceName,
            @Value("${factorx.news.http.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${factorx.news.http.read-timeout-ms:10000}") int readTimeoutMs
    ) {
        this.restClient = restClientBuilder.requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs)).build();
        this.normalizer = normalizer;
        this.feedUrl = feedUrl;
        this.sourceName = sourceName;
    }

    @Override
    public String sourceCode() {
        return "rss";
    }

    @Override
    public List<RawNewsItem> fetch(Instant since) {
        if (feedUrl.isBlank()) {
            throw new NewsSourceException("factorx.news.rss.url 未配置", false);
        }
        String xml;
        try {
            xml = restClient.get()
                    .uri(feedUrl)
                    .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            throw new NewsSourceException("RSS 请求失败", ex, isRetryable(ex));
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml == null ? new byte[0] : xml.getBytes()));
            NodeList items = document.getElementsByTagName("item");
            List<RawNewsItem> result = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = value(item, "title");
                String link = value(item, "link");
                String description = value(item, "description");
                Instant publishedAt = parseInstant(value(item, "pubDate"));
                if (title.isBlank() || link.isBlank() || publishedAt == null) {
                    continue;
                }
                if (since != null && !publishedAt.isAfter(since)) {
                    continue;
                }
                result.add(new RawNewsItem(
                        value(item, "guid"),
                        title,
                        description,
                        sourceName,
                        sourceCode(),
                        link,
                        publishedAt,
                        "en",
                        null,
                        null
                ));
            }
            return result;
        } catch (Exception ex) {
            throw new NewsSourceException("RSS XML 解析失败", ex, false);
        }
    }

    private String value(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : normalizer.text(nodes.item(0).getTextContent());
    }

    private Instant parseInstant(String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (Exception ignored) {
            try {
                return Instant.parse(value);
            } catch (Exception ignoredAgain) {
                return null;
            }
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
