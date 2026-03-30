// 감정 모델 registry 상태 변경 이력 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "감정 모델 registry 상태 변경 이력 응답 DTO")
public record EmotionModelRegistryStatusHistoryResponse(
        @Schema(description = "이력 id", example = "3")
        Long id,
        @Schema(description = "registry id", example = "1")
        Long registryId,
        @Schema(description = "이전 status", example = "APPROVED", nullable = true)
        String fromStatus,
        @Schema(description = "변경 후 status", example = "ACTIVE")
        String toStatus,
        @Schema(description = "상태 변경 사유", example = "activated")
        String changeReason,
        @Schema(description = "변경 시각", example = "2026-03-29T12:00:00")
        LocalDateTime changedAt
) {
}
