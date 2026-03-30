// 위험 신호 분류를 Spring AI로 시도하고 실패하면 규칙 기반 fallback으로 보호하는 서비스다.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.RiskScoreRequest;
import com.mindcompass.aiapi.dto.RiskScoreResponse;
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
public class RiskScoreService {

    private static final Logger log = LoggerFactory.getLogger(RiskScoreService.class);

    private static final Set<String> ALLOWED_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "NORMAL_RESPONSE",
            "SUPPORTIVE_RESPONSE",
            "SAFETY_RESPONSE"
    );
    private static final Set<String> ALLOWED_SIGNALS = Set.of(
            "SELF_HARM_IMPLICIT",
            "SELF_HARM_EXPLICIT",
            "ISOLATION",
            "DISTRESS_ESCALATION",
            "HOPELESSNESS"
    );
    private static final String SYSTEM_PROMPT = """
            You classify mental-health text into a conservative risk response for an internal safety system.
            Return JSON only with this schema:
            {
              \"riskLevel\": \"LOW|MEDIUM|HIGH\",
              \"riskScore\": 0.0-1.0,
              \"signals\": [\"SELF_HARM_IMPLICIT\"],
              \"recommendedAction\": \"NORMAL_RESPONSE|SUPPORTIVE_RESPONSE|SAFETY_RESPONSE\"
            }
            Allowed signals: SELF_HARM_IMPLICIT, SELF_HARM_EXPLICIT, ISOLATION, DISTRESS_ESCALATION, HOPELESSNESS.
            Use HIGH only for urgent self-harm or extreme crisis meaning.
            Use MEDIUM for significant distress, hopelessness, or isolation without explicit self-harm.
            Keep riskScore numerically consistent with riskLevel.
            LOW should usually stay around 0.00-0.34.
            MEDIUM should usually stay around 0.35-0.69.
            HIGH should usually stay around 0.70-1.00.
            Be conservative for safety, but do not exaggerate mild anxiety into HIGH.
            Do not include markdown fences or extra explanation.
            """;

    private final RiskScorePromptClient promptClient;
    private final ObjectMapper objectMapper;

    public RiskScoreService(RiskScorePromptClient promptClient, ObjectMapper objectMapper) {
        this.promptClient = promptClient;
        this.objectMapper = objectMapper;
    }

    public RiskScoreResponse score(RiskScoreRequest request) {
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            return lowRiskFallback();
        }

        RiskScoreResponse promptResponse = tryPromptScoring(request, text);
        if (promptResponse != null) {
            return promptResponse;
        }

        return ruleBasedFallback(text);
    }

    private RiskScoreResponse tryPromptScoring(RiskScoreRequest request, String text) {
        try {
            String raw = promptClient.complete(SYSTEM_PROMPT, buildUserPrompt(request, text));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return parseStructuredResponse(raw);
        } catch (RuntimeException exception) {
            log.warn("Risk score prompt call failed. sourceType={}", request.sourceType(), exception);
            return null;
        }
    }

    private String buildUserPrompt(RiskScoreRequest request, String text) {
        return """
                sourceType: %s
                text:
                %s
                """.formatted(
                request.sourceType(),
                text
        );
    }

    private RiskScoreResponse parseStructuredResponse(String raw) {
        try {
            String json = extractJsonObject(raw);
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {
            });

            String riskLevel = normalizeRiskLevel(payload.get("riskLevel"));
            BigDecimal riskScore = normalizeRiskScore(payload.get("riskScore"));
            String recommendedAction = normalizeAction(payload.get("recommendedAction"));
            List<String> signals = normalizeSignals(payload.get("signals"));

            if (riskLevel == null || riskScore == null || recommendedAction == null) {
                return null;
            }

            return new RiskScoreResponse(riskLevel, riskScore, signals, recommendedAction);
        } catch (Exception exception) {
            log.warn("Risk score response parsing failed. raw={}", raw, exception);
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

    private String normalizeRiskLevel(Object value) {
        if (!(value instanceof String riskLevelValue)) {
            return null;
        }
        String normalized = riskLevelValue.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_RISK_LEVELS.contains(normalized) ? normalized : null;
    }

    private BigDecimal normalizeRiskScore(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        double riskScore = number.doubleValue();
        if (riskScore < 0.0 || riskScore > 1.0) {
            return null;
        }
        return BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeAction(Object value) {
        if (!(value instanceof String actionValue)) {
            return null;
        }
        String normalized = actionValue.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ACTIONS.contains(normalized) ? normalized : null;
    }

    private List<String> normalizeSignals(Object value) {
        Set<String> signals = new LinkedHashSet<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof String signalValue) {
                    String normalized = signalValue.trim().toUpperCase(Locale.ROOT);
                    if (ALLOWED_SIGNALS.contains(normalized)) {
                        signals.add(normalized);
                    }
                }
            }
        }
        return List.copyOf(signals);
    }

    private RiskScoreResponse lowRiskFallback() {
        return new RiskScoreResponse(
                "LOW",
                BigDecimal.valueOf(0.05).setScale(2, RoundingMode.HALF_UP),
                List.of(),
                "NORMAL_RESPONSE"
        );
    }

    private RiskScoreResponse ruleBasedFallback(String text) {
        Set<String> highSignals = collectSignals(text, List.of(
                new SignalRule("SELF_HARM_IMPLICIT", List.of("죽고 싶", "사라지고 싶", "없어지고 싶")),
                new SignalRule("SELF_HARM_EXPLICIT", List.of("자해", "극단적 선택"))
        ));
        if (!highSignals.isEmpty()) {
            return new RiskScoreResponse(
                    "HIGH",
                    BigDecimal.valueOf(0.95).setScale(2, RoundingMode.HALF_UP),
                    List.copyOf(highSignals),
                    "SAFETY_RESPONSE"
            );
        }

        Set<String> mediumSignals = collectSignals(text, List.of(
                new SignalRule("ISOLATION", List.of("아무도 없", "혼자")),
                new SignalRule("DISTRESS_ESCALATION", List.of("버티기 힘들", "너무 힘들")),
                new SignalRule("HOPELESSNESS", List.of("희망이 없", "끝났어"))
        ));
        if (!mediumSignals.isEmpty()) {
            return new RiskScoreResponse(
                    "MEDIUM",
                    BigDecimal.valueOf(0.55).setScale(2, RoundingMode.HALF_UP),
                    List.copyOf(mediumSignals),
                    "SUPPORTIVE_RESPONSE"
            );
        }

        return new RiskScoreResponse(
                "LOW",
                BigDecimal.valueOf(0.10).setScale(2, RoundingMode.HALF_UP),
                List.of(),
                "NORMAL_RESPONSE"
        );
    }

    private Set<String> collectSignals(String text, List<SignalRule> rules) {
        Set<String> signals = new LinkedHashSet<>();
        for (SignalRule rule : rules) {
            for (String keyword : rule.keywords()) {
                if (text.contains(keyword)) {
                    signals.add(rule.signal());
                }
            }
        }
        return signals;
    }

    private record SignalRule(String signal, List<String> keywords) {
    }
}
