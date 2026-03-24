// 비교용 내부 AI 서버 상태를 확인하는 헬스 컨트롤러입니다.
package com.mindcompass.aiapi.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "ai-api",
                "runtime", "spring-ai"
        );
    }
}
