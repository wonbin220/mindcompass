// 감정 모델 registry 상태 변경 이력을 표현하는 도메인 레코드다.
package com.mindcompass.aiapi.registry.domain;

import java.time.LocalDateTime;

public record EmotionModelRegistryStatusHistoryRecord(
        Long id,
        Long registryId,
        String fromStatus,
        String toStatus,
        String changeReason,
        LocalDateTime changedAt
) {
}
