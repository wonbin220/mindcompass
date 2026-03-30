// 감정 분류 모델 registry 상태 전이 가능 범위를 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "감정 모델 registry 상태 전이 가능 범위 응답 DTO")
public record EmotionModelRegistryAvailableTransitionsResponse(
        @Schema(description = "registry id", example = "2")
        Long registryId,
        @Schema(description = "현재 status", example = "SHADOW")
        String currentStatus,
        @Schema(description = "현재 active serving row 여부", example = "false")
        boolean isActive,
        @Schema(description = "shadow lineage/source 여부", example = "true")
        boolean isShadow,
        @Schema(description = "status 변경 API에서 허용되는 다음 status 목록", example = "[\"APPROVED\", \"REJECTED\", \"ARCHIVED\"]")
        List<String> allowedStatusUpdates,
        @Schema(description = "activate API 호출 가능 여부", example = "false")
        boolean canActivate
) {
}
