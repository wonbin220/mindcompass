// ?곷떞 ?듬? ?앹꽦??Spring AI濡??쒕룄?섍퀬 ?ㅽ뙣?섎㈃ ?덉쟾??fallback?쇰줈 蹂댄샇?섎뒗 ?쒕퉬?ㅻ떎.
package com.mindcompass.aiapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.aiapi.dto.ConversationTurn;
import com.mindcompass.aiapi.dto.GenerateReplyRequest;
import com.mindcompass.aiapi.dto.GenerateReplyResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReplyGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ReplyGenerationService.class);

    private static final Set<String> ALLOWED_RESPONSE_TYPES = Set.of("NORMAL", "SUPPORTIVE", "SAFETY", "FALLBACK");
    private static final String EMPTY_MESSAGE_REPLY = "吏湲?留덉쓬????臾몄옣留????곸뼱二쇱떆硫??④퍡 ?뺣━?대낵 ???덉뼱??";
    private static final String SYSTEM_PROMPT = """
            You generate a supportive mental-health reply for an internal comparison server.
            Return JSON only with this schema:
            {
              \"reply\": \"string\",
              \"confidence\": 0.0-1.0,
              \"responseType\": \"NORMAL|SUPPORTIVE|SAFETY|FALLBACK\"
            }
            The reply must be 2-3 short Korean sentences.
            Sentence 1: empathize with the user's feeling.
            Sentence 2: reflect the user's situation or burden in concrete words.
            Final sentence: ask one small, low-pressure follow-up question.
            When possible, the final question should point to one concrete moment, trigger, or situation.
            Keep the tone calm, warm, natural, and non-judgmental.
            Avoid sounding robotic, repetitive, preachy, or overly generic.
            Avoid repeating the same emotion word twice unless necessary.
            Do not give harmful instructions or overconfident conclusions.
            Unless the user is clearly in urgent danger, prefer NORMAL.
            Do not include markdown fences or extra explanation.
            """;

    private final ReplyGenerationPromptClient promptClient;
    private final ObjectMapper objectMapper;

    public ReplyGenerationService(ReplyGenerationPromptClient promptClient, ObjectMapper objectMapper) {
        this.promptClient = promptClient;
        this.objectMapper = objectMapper;
    }

    public GenerateReplyResponse generate(GenerateReplyRequest request) {
        String message = request.message().trim();
        if (message.isEmpty()) {
            return fallbackResponse(EMPTY_MESSAGE_REPLY, BigDecimal.valueOf(0.15));
        }

        GenerateReplyResponse promptResponse = tryPromptReply(request, message);
        if (promptResponse != null) {
            return promptResponse;
        }

        return fallbackResponse(
                "'" + message + "'?쇨퀬 ?먮겮?④뎔?? 洹?留덉쓬??議곌툑 ???덉쟾?섍쾶 ??대궪 ???덈룄濡??④퍡 ?뺣━?대낵寃뚯슂.",
                BigDecimal.valueOf(0.45)
        );
    }

    private GenerateReplyResponse tryPromptReply(GenerateReplyRequest request, String message) {
        try {
            String raw = promptClient.complete(SYSTEM_PROMPT, buildUserPrompt(request, message));
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return parseStructuredResponse(raw);
        } catch (RuntimeException exception) {
            log.warn("Reply generation prompt call failed. sessionId={}, userId={}", request.sessionId(), request.userId(), exception);
            return null;
        }
    }

    private String buildUserPrompt(GenerateReplyRequest request, String message) {
        return """
                mode: %s
                memorySummary: %s
                latestMessage:
                %s

                recentConversation:
                %s
                """.formatted(
                request.mode() == null ? "null" : request.mode(),
                request.memorySummary() == null ? "null" : request.memorySummary(),
                message,
                formatConversationHistory(request.conversationHistory())
        );
    }

    private String formatConversationHistory(List<ConversationTurn> conversationHistory) {
        return conversationHistory.stream()
                .skip(Math.max(0, conversationHistory.size() - 4L))
                .map(turn -> turn.role() + ": " + turn.content())
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("none");
    }

    private GenerateReplyResponse parseStructuredResponse(String raw) {
        try {
            String json = extractJsonObject(raw);
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {
            });

            String reply = normalizeReply(payload.get("reply"));
            BigDecimal confidence = normalizeConfidence(payload.get("confidence"));
            String responseType = normalizeResponseType(payload.get("responseType"));

            if (reply == null || confidence == null || responseType == null) {
                return null;
            }

            return new GenerateReplyResponse(reply, confidence, responseType);
        } catch (Exception exception) {
            log.warn("Reply generation response parsing failed. raw={}", raw, exception);
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

    private String normalizeReply(Object value) {
        if (!(value instanceof String replyValue)) {
            return null;
        }
        String normalized = replyValue.trim();
        return normalized.isEmpty() ? null : normalized;
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

    private String normalizeResponseType(Object value) {
        if (!(value instanceof String responseTypeValue)) {
            return null;
        }
        String normalized = responseTypeValue.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_RESPONSE_TYPES.contains(normalized) ? normalized : null;
    }

    private GenerateReplyResponse fallbackResponse(String reply, BigDecimal confidence) {
        return new GenerateReplyResponse(
                reply,
                confidence.setScale(2, RoundingMode.HALF_UP),
                "FALLBACK"
        );
    }
}
