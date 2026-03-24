// 문장 속 위험 신호를 보수적으로 분류하는 서비스입니다.
package com.mindcompass.aiapi.service;

import com.mindcompass.aiapi.dto.RiskScoreRequest;
import com.mindcompass.aiapi.dto.RiskScoreResponse;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RiskScoreService {

    public RiskScoreResponse score(RiskScoreRequest request) {
        String text = request.text().trim();
        if (text.isEmpty()) {
            return new RiskScoreResponse("LOW", BigDecimal.valueOf(0.05), List.of(), "NORMAL_RESPONSE");
        }

        Set<String> highSignals = collectSignals(text, List.of(
                new SignalRule("SELF_HARM_IMPLICIT", List.of("죽고 싶", "사라지고 싶", "없애고 싶")),
                new SignalRule("SELF_HARM_EXPLICIT", List.of("자해", "극단적 선택"))
        ));
        if (!highSignals.isEmpty()) {
            return new RiskScoreResponse("HIGH", BigDecimal.valueOf(0.95), List.copyOf(highSignals), "SAFETY_RESPONSE");
        }

        Set<String> mediumSignals = collectSignals(text, List.of(
                new SignalRule("ISOLATION", List.of("아무도 없", "혼자")),
                new SignalRule("DISTRESS_ESCALATION", List.of("버티기 힘들", "너무 힘들")),
                new SignalRule("HOPELESSNESS", List.of("끝났", "희망이 없"))
        ));
        if (!mediumSignals.isEmpty()) {
            return new RiskScoreResponse("MEDIUM", BigDecimal.valueOf(0.65), List.copyOf(mediumSignals), "SUPPORTIVE_RESPONSE");
        }

        return new RiskScoreResponse("LOW", BigDecimal.valueOf(0.10), List.of(), "NORMAL_RESPONSE");
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
