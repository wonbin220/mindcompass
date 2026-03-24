// 위험 신호 스코어링 내부 엔드포인트를 제공하는 컨트롤러입니다.
package com.mindcompass.aiapi.controller;

import com.mindcompass.aiapi.dto.RiskScoreRequest;
import com.mindcompass.aiapi.dto.RiskScoreResponse;
import com.mindcompass.aiapi.service.RiskScoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class SafetyAiController {

    private final RiskScoreService riskScoreService;

    public SafetyAiController(RiskScoreService riskScoreService) {
        this.riskScoreService = riskScoreService;
    }

    @PostMapping("/risk-score")
    public RiskScoreResponse riskScore(@Valid @RequestBody RiskScoreRequest request) {
        return riskScoreService.score(request);
    }
}
