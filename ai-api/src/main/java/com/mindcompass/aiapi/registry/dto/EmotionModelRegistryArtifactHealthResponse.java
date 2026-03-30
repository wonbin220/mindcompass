// 감정 분류 모델 registry artifact 경로 점검 요약을 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "감정 모델 registry artifact health check 응답 DTO")
public record EmotionModelRegistryArtifactHealthResponse(
        @Schema(description = "registry id", example = "1")
        Long registryId,
        @Schema(description = "experiment 이름", example = "cpu_compare_medium_relabel_weighted")
        String experimentName,
        @Schema(description = "현재 registry status", example = "ACTIVE")
        String status,
        @Schema(description = "필수 artifact 경로가 모두 정상인지 여부", example = "true")
        boolean requiredArtifactsHealthy,
        @Schema(description = "설정된 경로 전체 기준으로 모두 정상인지 여부", example = "true")
        boolean overallHealthy,
        @Schema(description = "누락된 필수 점검 이름 목록", example = "[\"metricsJsonPath\"]")
        List<String> missingRequiredItems,
        @Schema(description = "존재하지 않는 선택 점검 이름 목록", example = "[\"labelMetadataPath\"]")
        List<String> missingOptionalItems,
        @Schema(description = "개별 artifact 경로 점검 결과 목록")
        List<EmotionModelRegistryArtifactHealthItemResponse> items
) {
}
