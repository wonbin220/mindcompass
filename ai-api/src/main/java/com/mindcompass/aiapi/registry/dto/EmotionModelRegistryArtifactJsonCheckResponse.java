// 감정 분류 모델 registry JSON artifact 파싱/스키마 점검 요약을 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "감정 모델 registry JSON artifact parse/schema check 응답 DTO")
public record EmotionModelRegistryArtifactJsonCheckResponse(
        @Schema(description = "registry id", example = "1")
        Long registryId,
        @Schema(description = "experiment 이름", example = "cpu_compare_medium_relabel_weighted")
        String experimentName,
        @Schema(description = "현재 registry status", example = "ACTIVE")
        String status,
        @Schema(description = "설정된 JSON artifact가 모두 파싱 가능한지 여부", example = "true")
        boolean parseHealthy,
        @Schema(description = "필수 JSON artifact가 최소 스키마를 만족하는지 여부", example = "true")
        boolean requiredSchemaHealthy,
        @Schema(description = "설정된 JSON artifact 전체가 최소 스키마를 만족하는지 여부", example = "true")
        boolean overallSchemaHealthy,
        @Schema(description = "파싱 실패한 항목 이름 목록", example = "[\"metricsJsonPath\"]")
        List<String> failedParseItems,
        @Schema(description = "스키마 검증 실패한 항목 이름 목록", example = "[\"labelMetadataPath\"]")
        List<String> failedSchemaItems,
        @Schema(description = "개별 JSON artifact 점검 결과 목록")
        List<EmotionModelRegistryArtifactJsonCheckItemResponse> items
) {
}
