// 감정 분류 모델 registry JSON artifact 개별 파싱/스키마 점검 결과를 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "감정 모델 registry JSON artifact 개별 점검 응답 DTO")
public record EmotionModelRegistryArtifactJsonCheckItemResponse(
        @Schema(description = "점검 대상 이름", example = "metricsJsonPath")
        String name,
        @Schema(description = "registry에 저장된 JSON 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted.json", nullable = true)
        String path,
        @Schema(description = "필수 JSON 여부", example = "true")
        boolean required,
        @Schema(description = "경로가 설정되어 있는지 여부", example = "true")
        boolean configured,
        @Schema(description = "파일이 존재하는지 여부", example = "true")
        boolean exists,
        @Schema(description = "JSON 파싱 성공 여부", example = "true")
        boolean parseable,
        @Schema(description = "최소 스키마 검증 성공 여부", example = "true")
        boolean schemaValid,
        @Schema(description = "필수 키 목록")
        List<String> requiredKeys,
        @Schema(description = "누락된 필수 키 목록")
        List<String> missingKeys,
        @Schema(description = "파싱 또는 스키마 오류 메시지", example = "Unexpected end-of-input", nullable = true)
        String errorMessage
) {
}
