package com.factorx.service;

import com.factorx.model.AnalysisRequest;
import com.factorx.model.ExtractedEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EventExtractorService {

    private static final String UNKNOWN = "待确认";

    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(?i)(?:(?:\\$|usd)\\s*)?(\\d+(?:\\.\\d+)?)\\s*(b|bn|billion|m|mn|million)(?:\\s*dollars?)?"
    );

    private static final Map<String, List<String>> SECTOR_KEYWORDS = new LinkedHashMap<>();
    private static final List<EventTypeRule> EVENT_TYPE_RULES = new ArrayList<>();
    private static final Map<String, String> COUNTRY_ALIASES = new LinkedHashMap<>();
    private static final Map<String, List<String>> COMPANY_ALIASES = new LinkedHashMap<>();
    private static final Map<String, Double> SOURCE_CREDIBILITY = new LinkedHashMap<>();

    static {
        SECTOR_KEYWORDS.put("新能源", List.of("solar", "battery", "ev", "energy storage"));
        SECTOR_KEYWORDS.put("AI 芯片", List.of("gpu", "ai chip", "data center", "accelerator"));
        SECTOR_KEYWORDS.put("半导体", List.of("semiconductor", "foundry", "lithography", "wafer"));
        SECTOR_KEYWORDS.put("能源", List.of("oil", "gas", "lng"));
        SECTOR_KEYWORDS.put("云计算/数据中心", List.of("cloud", "server", "data center"));

        EVENT_TYPE_RULES.add(new EventTypeRule("风险事件", List.of("ban", "bans", "sanction", "lawsuit", "probe", "recall")));
        EVENT_TYPE_RULES.add(new EventTypeRule("政策监管", List.of("subsidy", "policy", "regulation", "tax credit")));
        EVENT_TYPE_RULES.add(new EventTypeRule("订单合同", List.of("contract", "order", "deal", "supply agreement")));
        EVENT_TYPE_RULES.add(new EventTypeRule("国际项目", List.of("project", "factory", "plant", "build", "facility")));
        EVENT_TYPE_RULES.add(new EventTypeRule("投资合作", List.of("investment", "partnership", "joint venture")));

        COUNTRY_ALIASES.put("saudi arabia", "Saudi Arabia");
        COUNTRY_ALIASES.put("u.s.", "United States");
        COUNTRY_ALIASES.put("united states", "United States");
        COUNTRY_ALIASES.put("usa", "United States");
        COUNTRY_ALIASES.put("us", "United States");
        COUNTRY_ALIASES.put("china", "China");
        COUNTRY_ALIASES.put("germany", "Germany");
        COUNTRY_ALIASES.put("japan", "Japan");
        COUNTRY_ALIASES.put("europe", "Europe");
        COUNTRY_ALIASES.put("eu", "Europe");

        COMPANY_ALIASES.put("Tesla", List.of("tesla", "tsla"));
        COMPANY_ALIASES.put("Nvidia", List.of("nvidia", "nvda"));
        COMPANY_ALIASES.put("AMD", List.of("amd", "advanced micro devices"));
        COMPANY_ALIASES.put("ASML", List.of("asml"));
        COMPANY_ALIASES.put("TSMC", List.of("tsmc", "taiwan semiconductor"));

        SOURCE_CREDIBILITY.put("reuters", 0.90);
        SOURCE_CREDIBILITY.put("bloomberg", 0.88);
        SOURCE_CREDIBILITY.put("associated press", 0.86);
        SOURCE_CREDIBILITY.put("financial times", 0.85);
        SOURCE_CREDIBILITY.put("cnbc", 0.78);
        SOURCE_CREDIBILITY.put("unknown", 0.50);
    }

    public ExtractedEvent extract(AnalysisRequest request) {
        String originalText = normalizeWhitespace(nullToEmpty(request.headline()) + " " + nullToEmpty(request.body()));
        String text = originalText.toLowerCase(Locale.ROOT);
        String source = blankToDefault(request.source(), "Unknown");
        Set<String> keywords = new LinkedHashSet<>();

        Long projectAmountUsd = extractProjectAmountUsd(text, keywords);
        String sector = detectSector(text, keywords);
        String eventType = detectEventType(text, keywords);
        String country = detectCountry(text, keywords);
        List<String> companies = detectCompanies(text, keywords);

        return new ExtractedEvent(
                eventType,
                sector,
                country,
                projectAmountUsd,
                companies,
                List.copyOf(keywords),
                source,
                sourceCredibility(source)
        );
    }

    private Long extractProjectAmountUsd(String text, Set<String> keywords) {
        Matcher matcher = MONEY_PATTERN.matcher(text);
        Long bestAmount = null;
        String bestKeyword = null;

        while (matcher.find()) {
            BigDecimal amount = new BigDecimal(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            BigDecimal multiplier = unit.startsWith("b")
                    ? BigDecimal.valueOf(1_000_000_000L)
                    : BigDecimal.valueOf(1_000_000L);
            long usd = amount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).longValue();

            if (bestAmount == null || usd > bestAmount) {
                bestAmount = usd;
                bestKeyword = matcher.group().trim();
            }
        }

        if (bestKeyword != null) {
            keywords.add(bestKeyword);
        }
        return bestAmount;
    }

    private String detectSector(String text, Set<String> keywords) {
        return SECTOR_KEYWORDS.entrySet().stream()
                .map(entry -> new KeywordMatch(entry.getKey(), matchedKeywords(text, entry.getValue())))
                .filter(match -> !match.keywords().isEmpty())
                .max(Comparator.comparingInt((KeywordMatch match) -> match.keywords().size()))
                .map(match -> {
                    keywords.addAll(match.keywords());
                    return match.name();
                })
                .orElse(UNKNOWN);
    }

    private String detectEventType(String text, Set<String> keywords) {
        for (EventTypeRule rule : EVENT_TYPE_RULES) {
            List<String> matched = matchedKeywords(text, rule.keywords());
            if (!matched.isEmpty()) {
                keywords.addAll(matched);
                return rule.eventType();
            }
        }
        return UNKNOWN;
    }

    private String detectCountry(String text, Set<String> keywords) {
        for (Map.Entry<String, String> entry : COUNTRY_ALIASES.entrySet()) {
            if (containsKeyword(text, entry.getKey())) {
                keywords.add(entry.getValue());
                return entry.getValue();
            }
        }
        return UNKNOWN;
    }

    private List<String> detectCompanies(String text, Set<String> keywords) {
        List<String> companies = new ArrayList<>();

        COMPANY_ALIASES.forEach((company, aliases) -> {
            if (aliases.stream().anyMatch(alias -> containsKeyword(text, alias))) {
                companies.add(company);
                keywords.add(company);
            }
        });

        return companies.isEmpty() ? List.of(UNKNOWN) : companies;
    }

    private List<String> matchedKeywords(String text, List<String> candidates) {
        return candidates.stream()
                .filter(candidate -> containsKeyword(text, candidate))
                .toList();
    }

    private boolean containsKeyword(String text, String keyword) {
        String quoted = Pattern.quote(keyword.toLowerCase(Locale.ROOT));
        Pattern pattern = Pattern.compile("(?<![a-z0-9])" + quoted + "(?![a-z0-9])");
        return pattern.matcher(text).find();
    }

    private double sourceCredibility(String source) {
        return SOURCE_CREDIBILITY.getOrDefault(source.toLowerCase(Locale.ROOT), 0.50);
    }

    private String normalizeWhitespace(String value) {
        return nullToEmpty(value).replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record EventTypeRule(String eventType, List<String> keywords) {}

    private record KeywordMatch(String name, List<String> keywords) {}
}
