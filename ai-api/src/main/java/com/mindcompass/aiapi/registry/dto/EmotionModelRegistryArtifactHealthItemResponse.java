// 감정 분류 모델 registry artifact 개별 경로 점검 결과를 담는 응답 DTO다.
package com.mindcompass.aiapi.registry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "감정 모델 registry artifact 개별 경로 점검 응답 DTO")
public record EmotionModelRegistryArtifactHealthItemResponse(
        @Schema(description = "점검 대상 이름", example = "artifactDir")
        String name,
        @Schema(description = "registry에 저장된 경로", example = "C:/programing/mindcompass/ai-api-fastapi/training/emotion_classifier/artifacts/cpu_compare_medium_relabel_weighted_active5", nullable = true)
        String path,
        @Schema(description = "필수 경로 여부", example = "true")
        boolean required,
        @Schema(description = "경로가 설정되어 있는지 여부", example = "true")
        boolean configured,
        @Schema(description = "현재 파일시스템에서 경로가 존재하는지 여부", example = "true")
        boolean exists,
        @Schema(description = "디렉터리 기대 여부", example = "true")
        boolean directoryExpected,
        @Schema(description = "실제 디렉터리 여부", example = "true")
        boolean directory,
        @Schema(description = "경로 해석 또는 접근 오류 메시지", example = "Invalid path syntax", nullable = true)
        String errorMessage
) {
}
