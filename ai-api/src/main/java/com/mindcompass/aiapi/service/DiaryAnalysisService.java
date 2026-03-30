// ?쇨린 媛먯젙 遺꾩꽍??Spring AI濡??쒕룄?섍퀬 ?ㅽ뙣?섎㈃ 洹쒖튃 湲곕컲 fallback?쇰줈 蹂댄샇?섎뒗 ?쒕퉬?ㅻ떎.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.AnalyzeDiaryRequest;
import com.mindcompass.aiapi.dto.AnalyzeDiaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiaryAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DiaryAnalysisService.class);

    private static final String EMPTY_CONTENT_SUMMARY = "?댁슜??鍮꾩뼱 ?덉뼱 湲곕낯 媛먯젙 寃곌낵濡?諛섑솚?⑸땲??";
    private static final String ANXIOUS_SUMMARY = "遺덉븞怨?湲댁옣??諛섎났?섍퀬 ?쇱긽 吏묒쨷???대젮?좊뜕 湲곕줉?쇰줈 ?댁꽍?⑸땲??";
    private static final String DEFAULT_SUMMARY = "媛먯젙 ?먮쫫怨??쇱긽 留λ씫???④퍡 ?댄렣蹂??꾩슂媛 ?덈뒗 湲곕줉?낅땲??";
    private static final Set<String> ALLOWED_EMOTIONS = Set.of(
            "RELIEVED", "ANGRY", "SAD", "LONELY", "NUMB",
            "CALM", "HAPPY", "TIRED", "ANXIOUS", "OVERWHELMED", "TENSE"
    );
    private static final String SYSTEM_PROMPT = """
            You analyze a mental health diary entry for an internal wellness app.
            Return JSON only with this schema:
            {
              \"primaryEmotion\": \"ONE_UPPERCASE_LABEL\",
              \"emotionIntensity\": 1-5 integer,
              \"emotionTags\": [\"UPPERCASE_LABEL\"],
              \"summary\": \"Korean one-sentence summary\",
              \"confidence\": 0.0-1.0
            }
            Allowed labels: RELIEVED, ANGRY, SAD, LONELY, NUMB, CALM, HAPPY, TIRED, ANXIOUS, OVERWHELMED, TENSE.
            The summary must be one concise Korean sentence.
            The summary must mention emotion plus context, duration, or daily-life impact.
            Do not simply restate one emotion label like \"遺덉븞?덉뒿?덈떎.\"
            Prefer a natural sentence that a counselor could briefly read and understand.
            Do not include markdown fences or extra explanation.
            """;

    private final DiaryAnalysisPromptClient promptClient;
    private final ObjectMapper objectMapper;

    public DiaryAnalysisService(DiaryAnalysisPromptClient promptClient, ObjectMapper objectMapper) {
        this.promptClient = promptClient;
        this.objectMapper = objectMapper;
    }

    public AnalyzeDiaryResponse analyze(AnalyzeDiaryRequest request) {
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            return emptyContentFallback();
        }

        AnalyzeDiaryResponse promptResponse = tryPromptAnalysis(request, content);
        if (promptResponse != null) {
            return promptResponse;
        }

        return ruleBasedFallback(content);
    }

    private AnalyzeDiaryResponse tryPromptAnalysis(AnalyzeDiaryRequest request, String content) {
        try {
            String raw = promptClient.complete(SYSTEM_PROMPT, buildUserPrompt(request, content));
            if (raw == null || raw.isBlank()) {
                return null;
            }

            return parseStructuredResponse(raw);
        } catch (RuntimeException exception) {
            log.warn("Diary analysis prompt call failed. diaryId={}", request.diaryId(), exception);
            return null;
        }
    }

    private String buildUserPrompt(AnalyzeDiaryRequest request, String content) {
        return """
                writtenAt: %s
                content:
                %s
                """.formatted(request.writtenAt(), content);
    }

    private AnalyzeDiaryResponse parseStructuredResponse(String raw) {
        try {
            String json = extractJsonObject(raw);
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {
            });

            String primaryEmotion = normalizeEmotionValue(payload.get("primaryEmotion"));
            Integer emotionIntensity = normalizeIntensity(payload.get("emotionIntensity"));
            List<String> emotionTags = normalizeEmotionTags(payload.get("emotionTags"), primaryEmotion);
            String summary = normalizeSummary(payload.get("summary"), primaryEmotion);
            BigDecimal confidence = normalizeConfidence(payload.get("confidence"));

            if (primaryEmotion == null || emotionIntensity == null || summary == null || confidence == null) {
                return null;
            }

            return new AnalyzeDiaryResponse(primaryEmotion, emotionIntensity, emotionTags, summary, confidence);
        } catch (Exception exception) {
            log.warn("Diary analysis response parsing failed. raw={}", raw, exception);
            return null;
        }
    }

    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("JSON object not found in model response");
        }
        return raw.substring(start, end + 1);
    }

    private String normalizeEmotionValue(Object value) {
        if (!(value instanceof String emotionValue)) {
            return null;
        }

        String normalized = emotionValue.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_EMOTIONS.contains(normalized) ? normalized : null;
    }

    private Integer normalizeIntensity(Object value) {
        if (value instanceof Number number) {
            int intensity = number.intValue();
            return intensity >= 1 && intensity <= 5 ? intensity : null;
        }
        return null;
    }

    private List<String> normalizeEmotionTags(Object value, String primaryEmotion) {
        Set<String> tags = new LinkedHashSet<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String normalized = normalizeEmotionValue(item);
                if (normalized != null) {
                    tags.add(normalized);
                }
            }
        }

        if (primaryEmotion != null) {
            tags.add(primaryEmotion);
        }

        return List.copyOf(tags);
    }

    private String normalizeSummary(Object value, String primaryEmotion) {
        if (value instanceof String summaryValue && !summaryValue.isBlank()) {
            return summaryValue.trim();
        }

        if ("ANXIOUS".equals(primaryEmotion)) {
            return ANXIOUS_SUMMARY;
        }
        return DEFAULT_SUMMARY;
    }

    private BigDecimal normalizeConfidence(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }

        double confidence = number.doubleValue();
        if (confidence < 0.0 || confidence > 1.0) {
            return null;
        }
        return BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }

    private AnalyzeDiaryResponse emptyContentFallback() {
        return new AnalyzeDiaryResponse(
                "CALM",
                1,
                List.of("CALM"),
                EMPTY_CONTENT_SUMMARY,
                BigDecimal.valueOf(0.10).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private AnalyzeDiaryResponse ruleBasedFallback(String content) {
        if (containsAny(content, "遺덉븞", "嫄깆젙", "珥덉“")) {
            return new AnalyzeDiaryResponse(
                    "ANXIOUS",
                    4,
                    List.of("ANXIOUS", "TENSE"),
                    ANXIOUS_SUMMARY,
                    BigDecimal.valueOf(0.73).setScale(2, RoundingMode.HALF_UP)
            );
        }

        return new AnalyzeDiaryResponse(
                "CALM",
                2,
                List.of("CALM"),
                DEFAULT_SUMMARY,
                BigDecimal.valueOf(0.50).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
