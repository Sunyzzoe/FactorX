package com.factorx.news;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;

@Component
public class NewsTextNormalizer {

    public String text(String value) {
        if (value == null) {
            return "";
        }
        String withoutMarkup = value.replaceAll("<[^>]+>", " ");
        return Normalizer.normalize(withoutMarkup, Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String url(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(value.trim());
            return UriComponentsBuilder.fromUri(uri)
                    .replaceQueryParam("utm_source")
                    .replaceQueryParam("utm_medium")
                    .replaceQueryParam("utm_campaign")
                    .replaceQueryParam("utm_term")
                    .replaceQueryParam("utm_content")
                    .build()
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }

    public String hash(RawNewsItem item) {
        String canonical = text(item.title()).toLowerCase(Locale.ROOT)
                + "\n" + text(item.body()).toLowerCase(Locale.ROOT)
                + "\n" + (item.publishedAt() == null ? "" : item.publishedAt().toString());
        return sha256(canonical);
    }

    public String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
